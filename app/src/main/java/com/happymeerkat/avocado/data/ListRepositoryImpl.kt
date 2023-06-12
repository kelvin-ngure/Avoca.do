package com.happymeerkat.avocado.data

import com.happymeerkat.avocado.domain.model.ListItem
import com.happymeerkat.avocado.domain.repository.ListRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ListRepositoryImpl @Inject constructor(
    private val listDao: ListDao
): ListRepository {
    override fun getAllListItems(): Flow<List<ListItem>> = listDao.getAllListItems()

    override suspend fun upsertItem(listItem: ListItem) = listDao.upsertListItem(listItem)

    override suspend fun deleteItem(listItem: ListItem) = listDao.deleteListItem(listItem)
}