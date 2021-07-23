package com.ulsee.mower.ui.map


abstract class ActionRecordBoundary(val fragment: SetupMapFragment) {
    val binding = fragment.binding
    val context = fragment.context
    val viewModel = fragment.viewModel

    abstract fun execute()
}


class ActionNull(val subject: Int, val result: Int, fragment: SetupMapFragment): ActionRecordBoundary(fragment) {
    override fun execute() {
        // do nothing
    }
}
