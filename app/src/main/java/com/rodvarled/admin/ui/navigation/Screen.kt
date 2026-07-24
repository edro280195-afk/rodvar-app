package com.rodvarled.admin.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")

    object Dashboard : Screen("dashboard")

    object AppointmentsList : Screen("appointments")
    object AppointmentDetail : Screen("appointments/{id}") {
        fun of(id: Int) = "appointments/$id"
    }
    object AppointmentForm : Screen("appointments/form?id={id}") {
        fun create() = "appointments/form"
        fun edit(id: Int) = "appointments/form?id=$id"
    }
    object CompleteInstallation : Screen("appointments/{id}/complete") {
        fun of(id: Int) = "appointments/$id/complete"
    }

    object DirectInstallation : Screen("installations/direct")

    object WarrantiesList : Screen("warranties")
    object WarrantyDetail : Screen("warranties/{id}") {
        fun of(id: Int) = "warranties/$id"
    }

    object CustomersList : Screen("customers")
    object CustomerDetail : Screen("customers/{id}") {
        fun of(id: Int) = "customers/$id"
    }

    object InventoryList : Screen("inventory")
    object ProductDetail : Screen("inventory/{id}") {
        fun of(id: Int) = "inventory/$id"
    }
    object ProductForm : Screen("inventory/form?id={id}") {
        fun create() = "inventory/form"
        fun edit(id: Int) = "inventory/form?id=$id"
    }

    object QuotesList : Screen("quotes")
    object QuoteDetail : Screen("quotes/{id}") {
        fun of(id: Int) = "quotes/$id"
    }

    object More : Screen("more")
    object Compatibility : Screen("compatibility")
    object Reviews : Screen("reviews")
    object Gallery : Screen("gallery")
    object Profile : Screen("profile")
}
