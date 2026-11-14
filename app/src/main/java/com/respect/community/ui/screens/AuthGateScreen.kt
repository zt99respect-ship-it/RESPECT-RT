package com.respect.community.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.respect.community.data.local.UserProfileState
import com.respect.community.ui.components.GlassFilterChip
import com.respect.community.ui.components.HazeGlassCard
import com.respect.community.ui.components.RespectBrandLogo
import com.respect.community.ui.components.ScaleBounceButton
import com.respect.community.ui.theme.CyanAccent
import com.respect.community.ui.theme.GlassBorder
import com.respect.community.ui.theme.RespectPurpleBright
import com.respect.community.ui.theme.TextMuted
import com.respect.community.ui.theme.TextPrimary
import com.respect.community.ui.theme.TextSecondary
import com.respect.community.ui.theme.VoidBlack

@Composable
fun AuthGateScreen(
    profile: UserProfileState,
    isBusy: Boolean,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String) -> Unit,
    onResendVerification: () -> Unit,
    onCheckVerification: () -> Unit,
    onSignOut: () -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val needsVerification = profile.isFirebaseSignedIn && !profile.isEmailVerified && !profile.isAdmin

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RespectBrandLogo(size = 84.dp, cornerRadius = 22.dp)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("مجتمع RESPECT", style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = if (needsVerification) "تأكيد البريد الإلكتروني مطلوب" else "بوابة الدخول الآمن عبر Firebase",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            if (needsVerification) {
                HazeGlassCard(isGlowing = true) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MarkEmailRead,
                            contentDescription = null,
                            tint = RespectPurpleBright,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "تم إرسال رابط التفعيل إلى بريدك:",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = profile.email,
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "افتح الرسالة واضغط على رابط التحقق، ثم اضغط على زر التحقق أدناه للدخول إلى التطبيق.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        ScaleBounceButton(
                            onClick = onCheckVerification,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تم التفعيل — تحقق الآن")
                            }
                        }

                        OutlinedButton(
                            onClick = onResendVerification,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إعادة إرسال رابط التفعيل", color = TextPrimary)
                        }

                        TextButton(onClick = onSignOut) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل الخروج أو تغيير البريد", color = Color(0xFFFCA5A5))
                        }
                    }
                }
            } else {
                HazeGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassFilterChip(
                            label = "تسجيل الدخول",
                            selected = !isRegisterMode,
                            onClick = { isRegisterMode = false },
                            modifier = Modifier.weight(1f)
                        )
                        GlassFilterChip(
                            label = "حساب جديد",
                            selected = isRegisterMode,
                            onClick = { isRegisterMode = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RespectPurpleBright,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )

                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("الاسم المعروض") },
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = TextSecondary) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني") },
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = TextSecondary) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ScaleBounceButton(
                        onClick = {
                            if (isRegisterMode) {
                                onSignUp(email.trim(), password, displayName.trim())
                            } else {
                                onSignIn(email.trim(), password)
                            }
                        },
                        enabled = !isBusy && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isRegisterMode) "إنشاء الحساب وإرسال التفعيل" else "دخول للمجتمع")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRegisterMode) {
                            "سيتم إرسال رابط تحقق لبريدك الإلكتروني لتفعيل حسابك."
                        } else {
                            "أدخل بريدك الإلكتروني وكلمة المرور للدخول."
                        },
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
