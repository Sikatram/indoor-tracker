package com.example.cse218_fp_exp1.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {

    @Insert
    suspend fun insert(employeeEntity: EmployeeEntity)

    @Update
    suspend fun update(employeeEntity: EmployeeEntity)

    @Delete
    suspend fun delete(employeeEntity: EmployeeEntity)

    @Query("Select * from `Room_Object_table`")
    fun fetchAllEmployee():Flow<List<EmployeeEntity>>

    @Query("Select * from `Room_Object_table` where id=:id")
    fun fetchEmployeeById(id:Int):Flow<EmployeeEntity>
}