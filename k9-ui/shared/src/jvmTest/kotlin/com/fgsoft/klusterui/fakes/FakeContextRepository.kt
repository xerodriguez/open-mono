package com.fgsoft.klusterui.fakes

import com.fgsoft.klusterui.data.ContextRepository
import com.fgsoft.klusterui.data.Database

fun FakeContextRepository(database: Database) = ContextRepository(database)
