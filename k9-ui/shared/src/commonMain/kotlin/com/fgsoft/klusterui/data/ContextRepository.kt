package com.fgsoft.klusterui.data

import com.fgsoft.klusterui.model.KubeContext

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
}
