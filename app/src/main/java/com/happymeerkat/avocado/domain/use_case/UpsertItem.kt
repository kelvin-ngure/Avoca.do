package com.happymeerkat.avocado.domain.use_case

import com.happymeerkat.avocado.domain.model.ListItem
import com.happymeerkat.avocado.domain.repository.ListRepository

class UpsertItem(
    private val repository: ListRepository
) {
    suspend operator fun invoke(item: ListItem) {
        repository.upsertItem(item)
    }
}