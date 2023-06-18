package com.happymeerkat.avocado.presentation.vm

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happymeerkat.avocado.domain.model.Category
import com.happymeerkat.avocado.domain.model.ListItem
import com.happymeerkat.avocado.domain.use_case.ListUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
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
                            description = item.description ?: "",
                            categoryId = item.categoryId ?: 1,
                            dateMade = item.dateMade ?: 0,
                            dateDue = item.dateDue ?: 0,
                            timeDue = item.timeDue ?: 0,
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            _itemUIState.value = itemUIState.value.copy(timeDue = time.toEpochSecond(LocalDate.now(),ZoneOffset.UTC))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setDateDue(date: LocalDate) {
        _itemUIState.value = itemUIState.value.copy(dateDue = date.toEpochDay())
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNewItem(category: Category) {
        viewModelScope.launch {
            val item = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    title = _itemUIState.value.title,
                    description = _itemUIState.value.description,
                    categoryId = category.id, // TODO:change to
                    dateMade = _itemUIState.value.dateMade,
                    dateDue = _itemUIState.value.dateDue,
                    timeDue = _itemUIState.value.timeDue
                )
            } else {
                TODO("VERSION.SDK_INT < S")
            }
            upsertListItem(item)
        }
    }

    fun updateItem(item: ListItem?) {
        viewModelScope.launch {
            val currentItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    id = currentItemId,
                    title = _itemUIState.value.title,
                    description = _itemUIState.value.description,
                    categoryId = currentItemId,
                    dateMade = _itemUIState.value.dateMade,
                    dateDue = _itemUIState.value.dateDue,
                    timeDue = _itemUIState.value.timeDue
                )
            } else {
                TODO("VERSION.SDK_INT < S")
            }
            Log.d("CHECKING", item.toString())
            Log.d("CHECKING", currentItem.toString())
            upsertListItem(item ?: currentItem)
        }
    }
}

data class ItemUIState(
    val title: String = "",
    val description: String = "",
    val categoryId: Int = 1,
    val dateMade: Long? = 0,
    val dateDue: Long? = 0,
    val timeDue: Long? = 0,
    val completed: Boolean = false
)

