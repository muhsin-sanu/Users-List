package com.example.machinetest.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.machinetest.data.model.Address
import com.example.machinetest.data.model.Company
import com.example.machinetest.data.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: Int,
    val firstName: String,
    val lastName: String,
    val maidenName: String?,
    val age: Int,
    val gender: String,
    val email: String,
    val phone: String,
    val username: String,
    val image: String,
    val birthDate: String?,
    val bloodGroup: String?,
    val role: String?,
    val companyName: String?,
    val department: String?,
    val title: String?,
    val address: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?
) {
    fun toUser(): User {
        return User(
            id = id,
            firstName = firstName,
            lastName = lastName,
            maidenName = maidenName,
            age = age,
            gender = gender,
            email = email,
            phone = phone,
            username = username,
            image = image,
            birthDate = birthDate,
            bloodGroup = bloodGroup,
            role = role,
            company = if (companyName != null || department != null || title != null) {
                Company(
                    department = department,
                    name = companyName,
                    title = title
                )
            } else null,
            address = if (address != null || city != null || country != null) {
                Address(
                    address = address,
                    city = city,
                    state = state,
                    postalCode = postalCode,
                    country = country
                )
            } else null
        )
    }

    companion object {
        fun fromUser(user: User): UserEntity {
            return UserEntity(
                id = user.id,
                firstName = user.firstName,
                lastName = user.lastName,
                maidenName = user.maidenName,
                age = user.age,
                gender = user.gender,
                email = user.email,
                phone = user.phone,
                username = user.username,
                image = user.image,
                birthDate = user.birthDate,
                bloodGroup = user.bloodGroup,
                role = user.role,
                companyName = user.company?.name,
                department = user.company?.department,
                title = user.company?.title,
                address = user.address?.address,
                city = user.address?.city,
                state = user.address?.state,
                postalCode = user.address?.postalCode,
                country = user.address?.country
            )
        }
    }
}
