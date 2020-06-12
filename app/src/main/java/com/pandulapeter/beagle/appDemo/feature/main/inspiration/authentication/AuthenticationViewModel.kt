package com.pandulapeter.beagle.appDemo.feature.main.inspiration.authentication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pandulapeter.beagle.appDemo.R
import com.pandulapeter.beagle.appDemo.feature.main.inspiration.InspirationDetailViewModel
import com.pandulapeter.beagle.appDemo.feature.main.inspiration.authentication.list.AuthenticationListItem
import com.pandulapeter.beagle.appDemo.feature.main.inspiration.authentication.list.EmailInputViewHolder
import com.pandulapeter.beagle.appDemo.feature.main.inspiration.authentication.list.PasswordInputViewHolder
import com.pandulapeter.beagle.appDemo.feature.shared.list.CodeSnippetViewHolder
import com.pandulapeter.beagle.appDemo.feature.shared.list.TextViewHolder

class AuthenticationViewModel : InspirationDetailViewModel<AuthenticationListItem>() {

    private val _items = MutableLiveData<List<AuthenticationListItem>>()
    override val items: LiveData<List<AuthenticationListItem>> = _items
    private val text1 = TextViewHolder.UiModel(R.string.case_study_authentication_text_1)
    private val text2 = TextViewHolder.UiModel(R.string.case_study_authentication_text_2)
    private val snippet1 = CodeSnippetViewHolder.UiModel(
        "data class TestAccount(\n" +
                "    val email: String,\n" +
                "    val password: String\n" +
                ") : BeagleListItemContract {\n" +
                "\n" +
                "    override val id = email\n" +
                "    override val title = email\n" +
                "}"
    )
    private val text3 = TextViewHolder.UiModel(R.string.case_study_authentication_text_3)
    private val snippet2 = CodeSnippetViewHolder.UiModel(
        "ItemListModule(\n" +
                "    title = \"Test accounts\",\n" +
                "    items = listOf(\n" +
                "        TestAccount(\"email1\", \"pass1\"),\n" +
                "        TestAccount(\"email2\", \"pass2\"),\n" +
                "        TestAccount(\"email3\", \"pass3\"),\n" +
                "        TestAccount(\"email4\", \"pass4\")\n" +
                "    ),\n" +
                "    onItemSelected = { account ->\n" +
                "        emailInput.text = account.email\n" +
                "        passwordInput.text = account.password\n" +
                "        Beagle.hide()\n" +
                "        onSignInButtonPressed()\n" +
                "    }\n" +
                ")"
    )
    private val text4 = TextViewHolder.UiModel(R.string.case_study_authentication_text_4)
    private val snippet3 = CodeSnippetViewHolder.UiModel("Beagle.show()")
    private val text5 = TextViewHolder.UiModel(R.string.case_study_authentication_text_5)

    init {
        updateItems("", "")
    }

    fun updateItems(email: String, password: String) {
        _items.value = listOf(
            text1,
            EmailInputViewHolder.UiModel(email),
            PasswordInputViewHolder.UiModel(password),
            text2,
            snippet1,
            text3,
            snippet2,
            text4,
            snippet3,
            text5
        )
    }
}