package com.happymeerkat.avocado.presentation.vm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happymeerkat.avocado.MainActivity
import com.happymeerkat.avocado.domain.model.Category
import com.happymeerkat.avocado.domain.model.ListItem
import com.happymeerkat.avocado.domain.use_case.ListUseCases
import com.happymeerkat.avocado.notification.AlarmReceiver
import com.happymeerkat.avocado.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class EditItemVM @Inject constructor(
    private val listUseCases: ListUseCases,
    private val savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _itemUIState = MutableStateFlow(ItemUIState())
    val itemUIState: StateFlow<ItemUIState> = _itemUIState
    private var currentItemId: Int? = null
    init {
        savedStateHandle.get<Int>("id")?.let {id ->
            if(id != -1) {
                viewModelScope.launch {
                    listUseCases.getItemById(id)?.also {item ->
                        currentItemId = item.id
                        _itemUIState.value = itemUIState.value.copy(
                            title = item.title,
                            description = item.description,
                            categoryId = item.categoryId!!,
                            dateMade = item.dateMade,
                            dateDue = item.dateDue,
                            timeDue = item.timeDue,
                            completed = item.completed
                        )

                    }
                }
            }
        }
    }

    fun editTitle(newTitle: String) {
        _itemUIState.value = itemUIState.value.copy(title = newTitle)
    }
    fun editDescription(newDescription: String) {
        _itemUIState.value = itemUIState.value.copy(description = newDescription)
    }

    fun setTimeDue(time: LocalTime) {
            _itemUIState.value = itemUIState.value.copy(timeDue = time
                .toEpochSecond(
                    _itemUIState.value.selectedDate ?: LocalDate.ofEpochDay(itemUIState.value.dateDue!!),
                    ZoneId.systemDefault().rules.getOffset(Instant.now())
                )
            )
    }


    fun setDateDue(date: LocalDate) {
        _itemUIState.value = itemUIState.value.copy(dateDue = date.toEpochDay(), selectedDate = date)
        Log.d("DATE set as selected", _itemUIState.value.selectedDate.toString())
    }

    fun clearEditSlate() {
        _itemUIState.value = itemUIState.value.copy(
            dateDue = null,
            title = "",
            timeDue = null
        )
    }


    private suspend fun upsertListItem(item: ListItem) {
        listUseCases.upsertItem(item)

    }

    fun createNewItem(category: Category, context: Context) {
        viewModelScope.launch {
            val diff = (Date().time/1000) - Constants.sDate
            val item = ListItem(
                id = diff.toInt(),
                title = _itemUIState.value.title,
                description = _itemUIState.value.description,
                categoryId = category.id, // TODO:change to
                dateMade = getDateNowLong(),
                dateDue = _itemUIState.value.dateDue,
                timeDue = _itemUIState.value.timeDue
            )
            if(item.timeDue != null) setAlarm(item, context)

            upsertListItem(item)
        }
    }

    fun updateItem(context: Context) {

        viewModelScope.launch {
            val currentItem = currentItemId?.let {
                    ListItem(
                        id = it,
                        title = _itemUIState.value.title,
                        description = _itemUIState.value.description,
                        categoryId = _itemUIState.value.categoryId,
                        dateMade = _itemUIState.value.dateMade,
                        dateDue = _itemUIState.value.dateDue,
                        timeDue = _itemUIState.value.timeDue,
                        completed = _itemUIState.value.completed
                    )
                }
            if (currentItem != null) {
                removeAlarm(currentItem, context)
            }
            if (currentItem?.timeDue != null) {
                setAlarm(currentItem, context)
            }

            (currentItem)?.let { upsertListItem(it) }
        }
    }

    fun markCompleted(item: ListItem, context: Context?) {
        viewModelScope.launch {
            upsertListItem(item)
            if(item.timeDue != null) {
                removeAlarm(item, context!!)
            }
        }
    }

    fun setAlarm(listItem: ListItem, context: Context) {
        val timeNow = LocalTime.now().toEpochSecond(
            LocalDate.now(),
            ZoneId.systemDefault().rules.getOffset(Instant.now())
        )

        if(listItem.timeDue!! >= timeNow) { // only set an alarm if in future
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtra("list_item", listItem)
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND )
            val pendingIntent = PendingIntent.getBroadcast(context, listItem.id!!, intent, PendingIntent.FLAG_IMMUTABLE)
            val mainActivityIntent = Intent(context, MainActivity::class.java)
            val basicPendingIntent = PendingIntent.getActivity(context, listItem.id, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE)

            val seconds = listItem.timeDue
            val roundedToNearestMinute = (seconds/60)*60
            val milliseconds = roundedToNearestMinute.times(1000)
            val clockInfo = AlarmManager.AlarmClockInfo(milliseconds, basicPendingIntent)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
        }


    }

    fun removeAlarm(listItem: ListItem, context: Context){
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.putExtra("list_item", listItem)
        val pendingIntent = PendingIntent.getBroadcast(context, listItem.id!!, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getFormattedTime(time: Long): String {
        return Instant.ofEpochSecond(time).atZone(ZoneId.systemDefault().rules.getOffset(Instant.now())).toLocalTime().format(
            DateTimeFormatter.ofPattern("hh:mm a"))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getFormattedDate(date: Long): String {
        return LocalDate.ofEpochDay(date)
            .format(DateTimeFormatter.ofPattern("EEE, MMM dd"))
    }

    fun getDateNowLong(): Long {
        return System.currentTimeMillis()/1000
    }
}

data class ItemUIState(
    val title: String = "",
    val description: String? = null,
    val categoryId: Int = 1,
    val dateMade: Long? = null,
    val dateDue: Long? = null,
    val timeDue: Long? = null,
    val completed: Boolean = false,
    val selectedDate: LocalDate? = null
)

@HiltViewModel
class MainActVM @Inject constructor(
    private val listUseCases: ListUseCases
): ViewModel() {
    val visiblePermissionDialogueQueue = mutableListOf<String>()

    init {
        invokeDBCreation()
    }

    private fun invokeDBCreation() {
        viewModelScope.launch {
            listUseCases.categoryUpsert(Category(1, "All"))
        }

    }
    fun dismissDialog() {
        visiblePermissionDialogueQueue.removeLast()
    }

    fun onPermissionResult(
        permission: String,
        isGranted: Boolean
    ) {
        if(!isGranted) {
            visiblePermissionDialogueQueue.add(0, permission)
        }
    }
}