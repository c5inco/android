// "Add ''Parcelable'' supertype" "true"
// ERROR: No 'Parcelable' supertype
// WITH_STDLIB

package com.myapp.activity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class <caret>Test(val s: String)