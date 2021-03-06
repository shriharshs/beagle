package com.pandulapeter.beagle.common.contracts.module

import com.pandulapeter.beagle.common.configuration.Text
import com.pandulapeter.beagle.common.contracts.BeagleContract

/**
 * Modules that wrap values should implement this interface.
 * These modules support saving to / loading from Shared Preferences as well as bulk apply.
 * The save-load functionality is optional and will only work if the ID of the module is constant across app sessions.
 *
 * @param T - The type of data to persist.
 * @param M - The type of the module.
 */
interface ValueWrapperModule<T, M : Module<M>> : Module<M> {

    /**
     * The initial value.
     * If [isValuePersisted] is true, the value coming from the local storage will override this field so it will only be used the first time the app is launched.
     */
    val initialValue: T

    /**
     * The title of the module that can change in function of the current value.
     */
    val text: (T) -> Text

    /**
     * Can be used to enable or disable persisting the value on the local storage.
     */
    //TODO: Create a Lint warning to enforce overriding the module ID if this property is true.
    val isValuePersisted: Boolean

    /**
     * Can be used to enable / disable the module. Disabled modules are still visible on the UI but are static.
     */
    val isEnabled: Boolean

    /**
     * Can be used to enable or disable bulk apply. When enabled, changes made to the module by the user only take effect after a confirmation step.
     */
    val shouldRequireConfirmation: Boolean

    /**
     * Callback triggered when the user modifies the value.
     */
    val onValueChanged: (newValue: T) -> Unit

    /**
     * For every custom module a custom [Delegate] needs to be registered. Built-in modules use a different mechanism to achieve an empty implementation in the noop variant.
     *
     * This should always be overridden for custom modules.
     */
    override fun createModuleDelegate(): Delegate<T, M> = throw IllegalStateException("Built-in Modules should never create their own Delegates.")

    /**
     * Returns whether or not the module has any pending changes at the moment.
     * Not designed to be overridden.
     *
     * @param beagle - This should simply be the Beagle singleton.
     *
     * @return True if there are pending changes for the module, false otherwise.
     */
    @Suppress("UNCHECKED_CAST")
    fun hasPendingChanges(beagle: BeagleContract): Boolean = (beagle.delegateFor((this as M)::class) as? Delegate<T, M>?)?.hasPendingChanges(this as M) == true

    /**
     * Performs the pending change event after the user confirms it. The [hasPendingChanges] function is expected to return false after this call.
     * Not designed to be overridden.
     *
     * @param beagle - This should simply be the Beagle singleton.
     */
    @Suppress("UNCHECKED_CAST")
    fun applyPendingChanges(beagle: BeagleContract) = (beagle.delegateFor((this as M)::class) as? Delegate<T, M>?)?.applyPendingChanges(this as M)

    /**
     * Reverts the pending change event. The [hasPendingChanges] function is expected to return false after this call.
     * Not designed to be overridden.
     *
     * @param beagle - This should simply be the Beagle singleton.
     */
    @Suppress("UNCHECKED_CAST")
    fun resetPendingChanges(beagle: BeagleContract) = (beagle.delegateFor((this as M)::class) as? Delegate<T, M>?)?.resetPendingChanges(this as M)

    /**
     * Can be used to query the current value at any time.
     * Not designed to be overridden.
     *
     * @param beagle - This should simply be the Beagle singleton.
     *
     * @return - The current value or null if the module delegate was not found (this will be the case in the noop variant).
     */
    @Suppress("UNCHECKED_CAST")
    fun getCurrentValue(beagle: BeagleContract): T? = (beagle.delegateFor((this as M)::class) as? Delegate<T, M>?)?.getCurrentValue(this as M)

    /**
     * Can be used to update the current value at any time. Changes should also be reflected on the UI of the debug menu.
     * Not designed to be overridden.
     *
     * @param beagle - This should simply be the Beagle singleton.
     * @param newValue - The new value.
     */
    @Suppress("UNCHECKED_CAST")
    fun setCurrentValue(beagle: BeagleContract, newValue: T) = (beagle.delegateFor((this as M)::class) as? Delegate<T, M>?)?.setCurrentValue(this as M, newValue)

    /**
     * All [ValueWrapperModule] implementations must have their corresponding delegate that contains the implementation details.
     *
     * @param T - The type of data to persist.
     * @param M - The type of the module.
     */
    interface Delegate<T, M : Module<M>> : Module.Delegate<M> {

        /**
         * Returns whether or not the module has any pending changes at the moment.
         *
         * @param module - The module to check.
         *
         * @return True if there are pending changes for the module, false otherwise.
         */
        fun hasPendingChanges(module: M): Boolean

        /**
         * Performs the pending change event after the user confirms it. The [hasPendingChanges] function for the same module is expected to return false after this call.
         *
         * @param module - The module for which the changes should be applied.
         */
        fun applyPendingChanges(module: M)

        /**
         * Reverts the pending change event. The [hasPendingChanges] function for the same module is expected to return false after this call.
         *
         * @param module - The module for which the changes should be reset.
         */
        fun resetPendingChanges(module: M)

        /**
         * Returns the current value.
         */
        fun getCurrentValue(module: M): T

        /**
         * Updates the current value.
         */
        fun setCurrentValue(module: M, newValue: T)
    }
}