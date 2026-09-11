package com.example.machinetest.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UsersResponse(
    @SerializedName("users")
    val users: List<User> = emptyList(),
    @SerializedName("total")
    val total: Int = 0,
    @SerializedName("skip")
    val skip: Int = 0,
    @SerializedName("limit")
    val limit: Int = 0
)

data class User(
    @SerializedName("id")
    val id: Int,
    @SerializedName("firstName")
    val firstName: String = "",
    @SerializedName("lastName")
    val lastName: String = "",
    @SerializedName("maidenName")
    val maidenName: String? = null,
    @SerializedName("age")
    val age: Int = 0,
    @SerializedName("gender")
    val gender: String = "",
    @SerializedName("email")
    val email: String = "",
    @SerializedName("phone")
    val phone: String = "",
    @SerializedName("username")
    val username: String = "",
    @SerializedName("image")
    val image: String = "",
    @SerializedName("birthDate")
    val birthDate: String? = null,
    @SerializedName("bloodGroup")
    val bloodGroup: String? = null,
    @SerializedName("role")
    val role: String? = null,
    @SerializedName("company")
    val company: Company? = null,
    @SerializedName("address")
    val address: Address? = null
) : Serializable {
    val fullName: String
        get() = "$firstName $lastName".trim()

    val formattedRole: String
        get() = role?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "Member"
}

data class Company(
    @SerializedName("department")
    val department: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("title")
    val title: String? = null
) : Serializable

data class Address(
    @SerializedName("address")
    val address: String? = null,
    @SerializedName("city")
    val city: String? = null,
    @SerializedName("state")
    val state: String? = null,
    @SerializedName("postalCode")
    val postalCode: String? = null,
    @SerializedName("country")
    val country: String? = null
) : Serializable {
    val fullAddress: String
        get() = listOfNotNull(address, city, state, country).joinToString(", ")
}
