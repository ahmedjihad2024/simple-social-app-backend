package com.example.socialapp.account.lifecycle

import com.example.socialapp.account.enums.Role
import java.util.UUID

data class PersonaRoleDeactivated(val accountId: UUID, val role: Role)

data class PersonaRoleRestored(val accountId: UUID, val role: Role)

data class PersonaRolePurged(val accountId: UUID, val role: Role)
