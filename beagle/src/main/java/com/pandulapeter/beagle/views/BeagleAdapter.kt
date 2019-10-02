package com.pandulapeter.beagle.views

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pandulapeter.beagle.R
import com.pandulapeter.beagle.views.items.DrawerItemViewModel
import com.pandulapeter.beagle.views.items.button.ButtonListItemViewHolder
import com.pandulapeter.beagle.views.items.button.ButtonViewHolder
import com.pandulapeter.beagle.views.items.button.ButtonViewModel
import com.pandulapeter.beagle.views.items.header.HeaderViewHolder
import com.pandulapeter.beagle.views.items.header.HeaderViewModel
import com.pandulapeter.beagle.views.items.image.ImageViewHolder
import com.pandulapeter.beagle.views.items.image.ImageViewModel
import com.pandulapeter.beagle.views.items.keyValue.KeyValueItemViewHolder
import com.pandulapeter.beagle.views.items.keyValue.KeyValueItemViewModel
import com.pandulapeter.beagle.views.items.listHeader.ListHeaderViewHolder
import com.pandulapeter.beagle.views.items.listHeader.ListHeaderViewModel
import com.pandulapeter.beagle.views.items.listItem.ListItemViewHolder
import com.pandulapeter.beagle.views.items.listItem.ListItemViewModel
import com.pandulapeter.beagle.views.items.logItem.LogItemViewHolder
import com.pandulapeter.beagle.views.items.logItem.LogItemViewModel
import com.pandulapeter.beagle.views.items.longText.LongTextViewHolder
import com.pandulapeter.beagle.views.items.longText.LongTextViewModel
import com.pandulapeter.beagle.views.items.multipleSelectionListItem.MultipleSelectionListItemViewHolder
import com.pandulapeter.beagle.views.items.multipleSelectionListItem.MultipleSelectionListItemViewModel
import com.pandulapeter.beagle.views.items.networkLogItem.NetworkLogItemViewHolder
import com.pandulapeter.beagle.views.items.networkLogItem.NetworkLogItemViewModel
import com.pandulapeter.beagle.views.items.singleSelectionListItem.SingleSelectionListItemViewHolder
import com.pandulapeter.beagle.views.items.singleSelectionListItem.SingleSelectionListItemViewModel
import com.pandulapeter.beagle.views.items.slider.SliderViewHolder
import com.pandulapeter.beagle.views.items.slider.SliderViewModel
import com.pandulapeter.beagle.views.items.text.TextViewHolder
import com.pandulapeter.beagle.views.items.text.TextViewModel
import com.pandulapeter.beagle.views.items.toggle.ToggleViewHolder
import com.pandulapeter.beagle.views.items.toggle.ToggleViewModel

internal class BeagleAdapter : ListAdapter<DrawerItemViewModel, RecyclerView.ViewHolder>(object : DiffUtil.ItemCallback<DrawerItemViewModel>() {

    override fun areItemsTheSame(oldItem: DrawerItemViewModel, newItem: DrawerItemViewModel) = oldItem.id == newItem.id

    //TODO: Expanded list items are re-bound on every change for some reason.
    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: DrawerItemViewModel, newItem: DrawerItemViewModel) = oldItem == newItem

    override fun getChangePayload(oldItem: DrawerItemViewModel, newItem: DrawerItemViewModel): Any? = if (oldItem.shouldUsePayloads && newItem.shouldUsePayloads) "" else null
}) {

    override fun getItemViewType(position: Int) = when (val item = getItem(position)) {
        is TextViewModel -> R.layout.item_text
        is LongTextViewModel -> R.layout.item_long_text
        is ImageViewModel -> R.layout.item_image
        is SliderViewModel -> R.layout.item_slider
        is ToggleViewModel -> R.layout.item_toggle
        is ButtonViewModel -> if (item.shouldUseListItem) R.layout.item_button_list_item else R.layout.item_button
        is KeyValueItemViewModel -> R.layout.item_key_value
        is ListHeaderViewModel -> R.layout.item_list_header
        is ListItemViewModel<*> -> R.layout.item_list_item
        is SingleSelectionListItemViewModel<*> -> R.layout.item_single_selection_list_item
        is MultipleSelectionListItemViewModel<*> -> R.layout.item_multiple_selection_list_item
        is HeaderViewModel -> R.layout.item_header
        is NetworkLogItemViewModel -> R.layout.item_network_log
        is LogItemViewModel -> R.layout.item_log
        else -> throw IllegalArgumentException("Unsupported item type at position $position.")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        R.layout.item_text -> TextViewHolder.create(parent)
        R.layout.item_long_text -> LongTextViewHolder.create(parent)
        R.layout.item_image -> ImageViewHolder.create(parent)
        R.layout.item_slider -> SliderViewHolder.create(parent)
        R.layout.item_toggle -> ToggleViewHolder.create(parent)
        R.layout.item_button -> ButtonViewHolder.create(parent)
        R.layout.item_button_list_item -> ButtonListItemViewHolder.create(parent)
        R.layout.item_key_value -> KeyValueItemViewHolder.create(parent)
        R.layout.item_list_header -> ListHeaderViewHolder.create(parent)
        R.layout.item_list_item -> ListItemViewHolder.create(parent)
        R.layout.item_single_selection_list_item -> SingleSelectionListItemViewHolder.create(parent)
        R.layout.item_multiple_selection_list_item -> MultipleSelectionListItemViewHolder.create(parent)
        R.layout.item_header -> HeaderViewHolder.create(parent)
        R.layout.item_network_log -> NetworkLogItemViewHolder.create(parent)
        R.layout.item_log -> LogItemViewHolder.create(parent)
        else -> throw IllegalArgumentException("Unsupported view type: $viewType.")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = when (holder) {
        is TextViewHolder -> holder.bind(getItem(position) as TextViewModel)
        is LongTextViewHolder -> holder.bind(getItem(position) as LongTextViewModel)
        is ImageViewHolder -> holder.bind(getItem(position) as ImageViewModel)
        is SliderViewHolder -> holder.bind(getItem(position) as SliderViewModel)
        is ToggleViewHolder -> holder.bind(getItem(position) as ToggleViewModel)
        is ButtonViewHolder -> holder.bind(getItem(position) as ButtonViewModel)
        is ButtonListItemViewHolder -> holder.bind(getItem(position) as ButtonViewModel)
        is KeyValueItemViewHolder -> holder.bind(getItem(position) as KeyValueItemViewModel)
        is ListHeaderViewHolder -> holder.bind(getItem(position) as ListHeaderViewModel)
        is ListItemViewHolder -> holder.bind(getItem(position) as ListItemViewModel<*>)
        is SingleSelectionListItemViewHolder -> holder.bind(getItem(position) as SingleSelectionListItemViewModel<*>)
        is MultipleSelectionListItemViewHolder -> holder.bind(getItem(position) as MultipleSelectionListItemViewModel<*>)
        is HeaderViewHolder -> holder.bind(getItem(position) as HeaderViewModel)
        is NetworkLogItemViewHolder -> holder.bind(getItem(position) as NetworkLogItemViewModel)
        is LogItemViewHolder -> holder.bind(getItem(position) as LogItemViewModel)
        else -> throw IllegalArgumentException("Unsupported item type at position $position.")
    }
}