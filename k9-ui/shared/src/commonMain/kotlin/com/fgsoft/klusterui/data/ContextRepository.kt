package com.fgsoft.klusterui.data

import com.fgsoft.klusterui.model.FavoriteNamespace
import com.fgsoft.klusterui.model.KubeContext
import com.fgsoft.klusterui.model.SubContext

class ContextRepository(
    private val database: Database,
) {
    fun getAll(): List<KubeContext> = database.getAllContexts()

    fun getActive(): KubeContext? = database.getAllContexts().find { it.isActive }

    fun getAllActive(): List<KubeContext> = database.getActiveContexts()

    fun getById(id: Long): KubeContext? = database.getContext(id)

    fun create(context: KubeContext): Long = database.insertContext(context)

    fun update(context: KubeContext) = database.updateContext(context)

    fun delete(id: Long) = database.deleteContext(id)

    fun setActive(id: Long) = database.setActiveContext(id)

    fun deactivate(id: Long) = database.deactivateContext(id)

    fun getAllSubContexts(): List<SubContext> = database.getAllSubContexts()

    fun getSubContexts(contextId: Long): List<SubContext> = database.getSubContexts(contextId)

    fun createSubContext(subContext: SubContext): Long = database.insertSubContext(subContext)

    fun updateSubContext(subContext: SubContext) = database.updateSubContext(subContext)

    fun deleteSubContext(id: Long) = database.deleteSubContext(id)

    fun deleteSubContextsForContext(contextId: Long) = database.deleteSubContextsForContext(contextId)

    fun getAllFavoriteNamespaces(): List<FavoriteNamespace> = database.getAllFavoriteNamespaces()

    fun getFavoriteNamespaces(contextId: Long): List<FavoriteNamespace> = database.getFavoriteNamespaces(contextId)

    fun addFavoriteNamespace(fav: FavoriteNamespace): Long = database.insertFavoriteNamespace(fav)

    fun removeFavoriteNamespace(
        contextId: Long,
        namespace: String,
    ) = database.deleteFavoriteNamespace(contextId, namespace)

    fun isFavoriteNamespace(
        contextId: Long,
        namespace: String,
    ): Boolean = database.isFavoriteNamespace(contextId, namespace)
}
