package com.happymeerkat.avocado.presentation.screens.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.happymeerkat.avocado.domain.model.Category
import com.happymeerkat.avocado.presentation.screens.category.CategoryTabs
import com.happymeerkat.avocado.presentation.screens.dialog.DateDialog
import com.happymeerkat.avocado.presentation.screens.dialog.TimeDialog
import com.happymeerkat.avocado.presentation.screens.dialog.ConfirmationDialog
import com.happymeerkat.avocado.presentation.screens.dialog.CreateCategoryDialog
import com.happymeerkat.avocado.presentation.screens.dialog.EditCategoryDialog
import com.happymeerkat.avocado.presentation.screens.dialog.NewItemEditor
import com.happymeerkat.avocado.presentation.vm.MainVM
import com.vanpra.composematerialdialogs.rememberMaterialDialogState

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Home(
    modifier: Modifier = Modifier,
    navigateToDetails: (id: Int) -> Unit,
    viewModel: MainVM = hiltViewModel(),
    askNotificationsPermission: () -> Unit
) {
    askNotificationsPermission();
    val state = viewModel.mainUIState.collectAsState().value
    var editState by remember{ mutableStateOf(false) }
    var editCategory by remember{ mutableStateOf(false) }
    var createCategory by remember{ mutableStateOf(false) }
    var deleteCategory by remember{ mutableStateOf(false) }
    var deleteCompleted by remember{ mutableStateOf(false) }

    val dateDialogState = rememberMaterialDialogState()
    val timeDialogState = rememberMaterialDialogState()

    Box(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(8f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier.padding(start = 28.dp),
                        text = "My Lists",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "more options")
                    }
                }
            }
            CategoryTabs(
                modifier = Modifier.fillMaxWidth(),
                categories = state.categories,
                changeCurrentCategory = {category -> viewModel.changeCurrentCategory(category)},
                currentCategory = state.currentCategory,
                showEditDialog = {editCategory = true},
                showCreateNewCategoryModal = {createCategory = true}
            )

            ItemsList(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                listItems = if(state.currentCategory.name == "All") state.listItems.filter { !it.completed }
                else state.listItems.filter { (!it.completed) and (it.categoryId == state.currentCategory.id) },
                navigateToDetails = navigateToDetails,
                completedItems = if(state.currentCategory.name == "All") state.listItems.filter { it.completed }
                else state.listItems.filter { (it.completed) and (it.categoryId == state.currentCategory.id) },
                showDeleteCompletedItemsDialog = {deleteCompleted = true},
                currentCategory = state.currentCategory
            )

            if(!editState){
                BottomOptions(
                    toggleEditState = { editState = true },
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }


        if(editState){
            NewItemEditor(
                modifier = modifier
                    .align(Alignment.BottomCenter),
                closeModal = { editState = false },
                currentCategory = state.currentCategory,
                showDateDialog = {dateDialogState.show()}
            )
        }

        if(editCategory) {
            EditCategoryDialog(
                closeModal = {editCategory = false},
                category = state.currentCategory,
                showConfirmationDialog = {deleteCategory = true},
                editCategoryName = {newName -> viewModel.editCurrentCategoryName(newName)}
            )
        }

        if(createCategory) {
            CreateCategoryDialog(
                createCategory = { category: Category -> viewModel.createNewCategory(category) },
                closeModal = {createCategory = false},
                changeCurrentActiveCategory = {viewModel.changeCurrentCategory(state.categories.last())}
            )
        }

        if(deleteCategory) {
            ConfirmationDialog(
                title = "Are you sure?",
                message = "Deleting category \"${state.currentCategory.name}\" will also delete all items associated with it?",
                functionToRun = { viewModel.deleteCurrentCategory()},
                closeModal = { deleteCategory = false }
            )
        }
        
        if(deleteCompleted) {
            ConfirmationDialog(
                title = "Are you sure?",
                message = "Delete completed tasks in category: \"${state.currentCategory.name}\" ?",
                functionToRun = { viewModel.deleteCompletedTasks()},
                closeModal = { deleteCompleted = false }
            )
        }

        DateDialog(dateDialogState = dateDialogState, openTimeDialog = {timeDialogState.show()})
        TimeDialog(timeDialogState = timeDialogState)




    }
}