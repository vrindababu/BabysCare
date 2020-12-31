package net.babys_care.app.scene.registration

import jp.winas.android.foundation.scene.BaseViewModel
import net.babys_care.app.models.Parent

class ParentRegistrationViewModel: BaseViewModel() {
    val parentUser = Parent("", "", "",
        "", "", "", "",
        0, "", "")
    var email: String = ""
    var password: String = ""
}