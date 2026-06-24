package com.fgsoft.klusterui.store

import com.fgsoft.klusterui.fakes.FakeDatabase
import com.fgsoft.klusterui.fakes.fakeDeps
import com.fgsoft.klusterui.model.KubeContext
import com.fgsoft.klusterui.ui.store.ContextStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ContextStoreTest {
    private lateinit var db: FakeDatabase
    private lateinit var store: ContextStore

    @BeforeTest
    fun setUp() {
        db = FakeDatabase()
        val deps = fakeDeps(db)
        store = ContextStore(deps, CoroutineScope(UnconfinedTestDispatcher()))
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun `load populates allContexts and activeContexts`() {
        db.insertContext(KubeContext(id = 0, name = "ctx1", context = "k8s-ctx1", isActive = true))
        db.insertContext(KubeContext(id = 0, name = "ctx2", context = "k8s-ctx2", isActive = false))

        store.load()

        assertEquals(2, store.allContexts.size)
        assertEquals(1, store.activeContexts.size)
        assertEquals("ctx1", store.activeContexts[0].name)
    }

    @Test
    fun `add creates context and reloads`() {
        store.load()

        store.add(KubeContext(name = "new-ctx", context = "k8s-new"))

        assertEquals(1, store.allContexts.size)
        assertEquals("new-ctx", store.allContexts[0].name)
    }

    @Test
    fun `update modifies context and reloads`() {
        val id = db.insertContext(KubeContext(id = 0, name = "old", context = "k8s-old"))
        store.load()

        store.update(KubeContext(id = id, name = "updated", context = "k8s-updated"))

        assertEquals("updated", store.allContexts[0].name)
        assertEquals("k8s-updated", store.allContexts[0].context)
    }

    @Test
    fun `delete removes context and reloads`() {
        val id = db.insertContext(KubeContext(id = 0, name = "ctx", context = "k8s-ctx"))
        store.load()
        assertEquals(1, store.allContexts.size)

        store.delete(id)

        assertEquals(0, store.allContexts.size)
    }

    @Test
    fun `activate sets context active and reloads`() {
        val id = db.insertContext(KubeContext(id = 0, name = "ctx", context = "k8s-ctx", isActive = false))
        store.load()
        assertEquals(0, store.activeContexts.size)

        store.activate(KubeContext(id = id, name = "ctx", context = "k8s-ctx"))

        assertEquals(1, store.activeContexts.size)
        assertTrue(store.activeContexts[0].isActive)
    }

    @Test
    fun `toggleActive flips isActive`() {
        val id = db.insertContext(KubeContext(id = 0, name = "ctx", context = "k8s-ctx", isActive = false))
        store.load()

        store.toggleActive(KubeContext(id = id, name = "ctx", context = "k8s-ctx", isActive = false))

        assertEquals(1, store.activeContexts.size)
        assertTrue(store.activeContexts[0].isActive)

        store.toggleActive(KubeContext(id = id, name = "ctx", context = "k8s-ctx", isActive = true))

        assertEquals(0, store.activeContexts.size)
    }

    @Test
    fun `toggleFavorite adds then removes`() {
        store.load()

        store.toggleFavorite(1, "default")

        assertTrue(store.isFavorite(1, "default"))

        store.toggleFavorite(1, "default")

        assertFalse(store.isFavorite(1, "default"))
    }

    @Test
    fun `isFavorite returns false for unknown`() {
        store.load()

        assertFalse(store.isFavorite(1, "nonexistent"))
    }

    @Test
    fun `favoriteNamespacesFor returns set of namespaces`() {
        store.load()
        store.toggleFavorite(1, "default")
        store.toggleFavorite(1, "kube-system")

        val result = store.favoriteNamespacesFor(1)

        assertEquals(setOf("default", "kube-system"), result)
    }

    @Test
    fun `favoriteNamespacesFor empty when no favorites`() {
        store.load()

        assertEquals(emptySet(), store.favoriteNamespacesFor(1))
    }
}
