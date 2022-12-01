package com.example.cse218_fp_exp1.ui.pin

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cse218_fp_exp1.MainActivity
import com.example.cse218_fp_exp1.R
import com.example.cse218_fp_exp1.databinding.DialogUpdateBinding
import com.example.cse218_fp_exp1.databinding.FragmentPinBinding
import com.example.cse218_fp_exp1.db.EmployeeApp
import com.example.cse218_fp_exp1.db.EmployeeDao
import com.example.cse218_fp_exp1.db.EmployeeEntity
import com.example.cse218_fp_exp1.db.ItemAdapter
import kotlinx.coroutines.launch


class PinFragment : Fragment() {

    private var _binding: FragmentPinBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val pinViewModel =
            ViewModelProvider(this)[PinViewModel::class.java]

        _binding = FragmentPinBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Todo 9: get the employeeDao variable through the application class
        if (requireActivity().parent == null ){
            Log.e("estimote", "parent null")
        } else {
            Log.e("estimote", requireActivity().parent.toString())
        }
        val employeeDao = (requireActivity() as MainActivity).db!!.employeeDao()
        binding.btnAdd.setOnClickListener {
            //Todo 10 pass in the employeDao
            addRecord(employeeDao)
        }
        //Todo  11 launch a coroutine block and fetch all employee
        lifecycleScope.launch {
            employeeDao.fetchAllEmployee().collect {
                Log.d("exactemployee", "$it")
                val list = ArrayList(it)
                setupListOfDataIntoRecyclerView(list,employeeDao)
            }
        }
        setFragmentResultListener("map pin") { _, bundle ->
            val result = bundle.getDoubleArray("user position")
            // Do something with the result
            val x = result!![0]
            val y = result[1]
        }
    }
    //Todo 1 create an employeeDao param to access the insert method
    //launch a coroutine block to call the method for inserting entry
    fun addRecord(employeeDao: EmployeeDao) {
        val name = binding.etName.text.toString()
        val xCoord = binding.etXcoordinate.text.toString()
        val yCoord = binding.etYcoordinate.text.toString()
        if (name.isNotEmpty() && xCoord.isNotEmpty() && yCoord.isNotEmpty()) {
            lifecycleScope.launch {
                employeeDao.insert(EmployeeEntity(name = name, xCoordinate = xCoord, yCoordinate = yCoord))
                Toast.makeText(requireContext(), "Record saved", Toast.LENGTH_LONG).show()
                binding?.etName?.text?.clear()
                binding?.etXcoordinate?.text?.clear()
                binding?.etYcoordinate?.text?.clear()

            }
        } else {
            Toast.makeText(
                requireContext(),
                "Name or Coordinates cannot be blank",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    /** Todo 4: create an employee param to pass into updateRecordDialog and updateRecordDialog
     *  method
     * Function is used show the list of inserted data.
     */
    private fun setupListOfDataIntoRecyclerView(employeesList:ArrayList<EmployeeEntity>,
                                                employeeDao: EmployeeDao) {

        if (employeesList.isNotEmpty()) {


            // Adapter class is initialized and list is passed in the param.
            val itemAdapter = ItemAdapter(employeesList,{updateId ->
                updateRecordDialog(updateId,employeeDao)
            }){ deleteId->
                lifecycleScope.launch {
                    employeeDao.fetchEmployeeById(deleteId).collect {
                        if (it != null) {
                            deleteRecordAlertDialog(deleteId, employeeDao, it)
                        }
                    }
                }

            }
            // Set the LayoutManager that this RecyclerView will use.
            binding?.rvItemsList?.layoutManager = LinearLayoutManager(requireContext())
            // adapter instance is set to the recyclerview to inflate the items.
            binding?.rvItemsList?.adapter = itemAdapter
            binding?.rvItemsList?.visibility = View.VISIBLE
            binding?.tvNoRecordsAvailable?.visibility = View.GONE
        } else {

            binding?.rvItemsList?.visibility = View.GONE
            binding?.tvNoRecordsAvailable?.visibility = View.VISIBLE
        }
    }

    /**Todo 5:  create an id param for identifying the row to be updated
     * Create an employeeDao param for accessing method from the dao
     * We also launch a coroutine block to fetch the selected employee and update it
     */
    fun updateRecordDialog(id:Int,employeeDao: EmployeeDao)  {
        val updateDialog = Dialog(requireContext(), R.style.Theme_Cse218_fp_exp1)
        updateDialog.setCancelable(false)
        /*Set the screen content from a layout resource.
         The resource will be inflated, adding all top-level views to the screen.*/
        val binding = DialogUpdateBinding.inflate(layoutInflater)
        updateDialog.setContentView(binding.root)

        lifecycleScope.launch {
            employeeDao.fetchEmployeeById(id).collect {
                if (it != null) {
                    binding.etUpdateName.setText(it.name)
                    binding.etUpdateXcoord.setText(it.xCoordinate)
                    binding.etUpdateYcoord.setText(it.yCoordinate)
                }
            }
        }
        binding.tvUpdate.setOnClickListener {

            val name = binding.etUpdateName.text.toString()
            val email = binding.etUpdateXcoord.text.toString()
            val yCoord = binding.etUpdateYcoord.text.toString()

            if (name.isNotEmpty() && email.isNotEmpty() && yCoord.isNotEmpty()) {
                lifecycleScope.launch {
                    employeeDao.update(EmployeeEntity(id, name, email, yCoord))
                    Toast.makeText(requireContext(), "Record Updated.", Toast.LENGTH_LONG)
                        .show()
                    updateDialog.dismiss() // Dialog will be dismissed
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Name or Coordinates cannot be blank",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        binding.tvCancel.setOnClickListener{
            updateDialog.dismiss()
        }
        //Start the dialog and display it on screen.
        updateDialog.show()
    }


    /** Todo 6
     * Method is used to show the Alert Dialog and delete the selected employee.
     * We add an id to get the selected position and an employeeDao param to get the
     * methods from the dao interface then launch a coroutine block to call the methods
     */
    fun deleteRecordAlertDialog(id:Int, employeeDao: EmployeeDao, employee: EmployeeEntity) {
        val builder = AlertDialog.Builder(requireContext())
        //set title for alert dialog
        builder.setTitle("Delete Record")
        //set message for alert dialog
        builder.setMessage("Are you sure you wants to delete ${employee.name}.")
        builder.setIcon(android.R.drawable.ic_dialog_alert)

        //performing positive action
        builder.setPositiveButton("Yes") { dialogInterface, _ ->
            lifecycleScope.launch {
                employeeDao.delete(EmployeeEntity(id))
                Toast.makeText(
                    requireContext(),
                    "Record deleted successfully.",
                    Toast.LENGTH_LONG
                ).show()

                dialogInterface.dismiss() // Dialog will be dismissed
            }

        }

        //performing negative action
        builder.setNegativeButton("No") { dialogInterface, which ->
            dialogInterface.dismiss() // Dialog will be dismissed
        }
        // Create the AlertDialog
        val alertDialog: AlertDialog = builder.create()
        // Set other dialog properties
        alertDialog.setCancelable(false) // Will not allow user to cancel after clicking on remaining screen area.
        alertDialog.show()  // show the dialog to UI
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}