package com.example.cse218_fp_exp1.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

//creating a Data Model Class
@Entity(tableName = "Room_Object_table")
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int=0,
    val name: String="",
    @ColumnInfo(name = "x_coordinate")
    val xCoordinate: String="",
    @ColumnInfo(name = "y_coordinate")
    val yCoordinate: String=""
)
