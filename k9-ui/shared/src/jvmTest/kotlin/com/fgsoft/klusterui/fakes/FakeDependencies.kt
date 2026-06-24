package com.fgsoft.klusterui.fakes

import com.fgsoft.klusterui.AppDependencies
import com.fgsoft.klusterui.data.PortForwardRepository

fun fakeAppDependencies(): AppDependencies {
    val db = FakeDatabase()
    return AppDependencies(
        database = db,
        contextRepository = FakeContextRepository(db),
        portForwardRepository = PortForwardRepository(db),
        kubectlClient = FakeKubectlClient(),
        processManager = FakeProcessManager(),
    )
}

fun fakeDeps(db: FakeDatabase): AppDependencies =
    AppDependencies(
        database = db,
        contextRepository = FakeContextRepository(db),
        portForwardRepository = PortForwardRepository(db),
        kubectlClient = FakeKubectlClient(),
        processManager = FakeProcessManager(),
    )
