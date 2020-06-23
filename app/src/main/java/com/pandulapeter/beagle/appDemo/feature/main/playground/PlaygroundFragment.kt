package com.pandulapeter.beagle.appDemo.feature.main.playground

import androidx.lifecycle.viewModelScope
import com.pandulapeter.beagle.appDemo.R
import com.pandulapeter.beagle.appDemo.feature.main.playground.addModule.AddModuleFragment
import com.pandulapeter.beagle.appDemo.feature.main.playground.list.PlaygroundAdapter
import com.pandulapeter.beagle.appDemo.feature.main.playground.list.PlaygroundListItem
import com.pandulapeter.beagle.appDemo.feature.shared.ListFragment
import com.pandulapeter.beagle.appDemo.utils.TransitionType
import com.pandulapeter.beagle.appDemo.utils.handleReplace
import com.pandulapeter.beagle.appDemo.utils.showSnackbar
import com.pandulapeter.beagle.common.contracts.BeagleListItemContract
import com.pandulapeter.beagle.common.contracts.module.Module
import com.pandulapeter.beagle.modules.AnimationDurationSwitchModule
import com.pandulapeter.beagle.modules.AppInfoButtonModule
import com.pandulapeter.beagle.modules.ButtonModule
import com.pandulapeter.beagle.modules.CheckBoxModule
import com.pandulapeter.beagle.modules.DeviceInfoModule
import com.pandulapeter.beagle.modules.DividerModule
import com.pandulapeter.beagle.modules.ForceCrashButtonModule
import com.pandulapeter.beagle.modules.HeaderModule
import com.pandulapeter.beagle.modules.ItemListModule
import com.pandulapeter.beagle.modules.KeyValueListModule
import com.pandulapeter.beagle.modules.LabelModule
import com.pandulapeter.beagle.modules.LongTextModule
import com.pandulapeter.beagle.modules.MultipleSelectionListModule
import com.pandulapeter.beagle.modules.PaddingModule
import com.pandulapeter.beagle.modules.SingleSelectionListModule
import com.pandulapeter.beagle.modules.SwitchModule
import com.pandulapeter.beagle.modules.TextModule
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlaygroundFragment : ListFragment<PlaygroundViewModel, PlaygroundListItem>(R.string.playground_title) {

    override val viewModel by viewModel<PlaygroundViewModel>()

    override fun createAdapter() = PlaygroundAdapter(
        scope = viewModel.viewModelScope,
        onAddModuleClicked = ::navigateToAddModule,
        onGenerateCodeClicked = ::generateCode
    )

    override fun getBeagleModules(): List<Module<*>> = listOf(
        ButtonModule(text = "Button", onButtonPressed = {}),
        CheckBoxModule(text = "Checkbox", onValueChanged = {}),
        DividerModule(),
        ItemListModule(
            title = "ItemList",
            items = listOf(
                ListItem("Item 1"),
                ListItem("Item 2"),
                ListItem("Item 3")
            ),
            onItemSelected = {}
        ),
        KeyValueListModule(
            title = "KeyValueList",
            pairs = listOf(
                "Key 1" to "Value 1",
                "Key 2" to "Value 2",
                "Key 3" to "Value 3"
            )
        ),
        LabelModule(title = "Label"),
        LongTextModule(
            title = "LongText",
            text = "This is a longer piece of text that only becomes visible when the user expands the header."
        ),
        MultipleSelectionListModule(
            title = "MultipleSelectionList",
            items = listOf(
                ListItem("Checkbox 1"),
                ListItem("Checkbox 2"),
                ListItem("Checkbox 3")
            ),
            initiallySelectedItemIds = emptySet(),
            onSelectionChanged = {}
        ),
        PaddingModule(),
        SingleSelectionListModule(
            title = "SingleSelectionList",
            items = listOf(
                ListItem("Radio button 1"),
                ListItem("Radio button 2"),
                ListItem("Radio button 3")
            ),
            initiallySelectedItemId = null,
            onSelectionChanged = {}
        ),
        SwitchModule(text = "Switch", onValueChanged = {}),
        TextModule(text = "Text"),
        AnimationDurationSwitchModule(),
        AppInfoButtonModule(),
        DeviceInfoModule(),
        ForceCrashButtonModule(),
        HeaderModule(
            title = "Header title",
            subtitle = "Header subtitle"
        )
    )

    private fun navigateToAddModule() = parentFragment?.childFragmentManager?.handleReplace(
        transitionType = TransitionType.MODAL,
        addToBackStack = true,
        newInstance = AddModuleFragment.Companion::newInstance
    ) ?: Unit

    private fun generateCode() = binding.recyclerView.showSnackbar(R.string.coming_soon) //TODO: Open the dialog with the code snippet

    private data class ListItem(
        override val title: String
    ) : BeagleListItemContract {

        override val id = title
    }

    companion object {
        fun newInstance() = PlaygroundFragment()
    }
}