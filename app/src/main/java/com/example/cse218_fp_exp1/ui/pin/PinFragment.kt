package com.example.cse218_fp_exp1.ui.pin

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import com.example.cse218_fp_exp1.db.EmployeeDao
import com.example.cse218_fp_exp1.db.EmployeeEntity
import com.example.cse218_fp_exp1.db.ItemAdapter
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
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
            if (x != -69.0 && y != -69.0) {
                binding.etXcoordinate.text =
                    Editable.Factory.getInstance().newEditable(String.format("%.2f", x))
                binding.etYcoordinate.text =
                    Editable.Factory.getInstance().newEditable(String.format("%.2f", y))
            }
        }
    }
    //Todo 1 create an employeeDao param to access the insert method
    //launch a coroutine block to call the method for inserting entry
    fun addRecord(employeeDao: EmployeeDao) {
        val name = binding.etName.text.toString()
        val xCoord = binding.etXcoordinate.text.toString()
        val yCoord = binding.etYcoordinate.text.toString()

        try {
            xCoord.toDouble()
            yCoord.toDouble()
        } catch (e: java.lang.NumberFormatException) {
            var snackbar = Snackbar.make(requireView(), "X and Y Coordinates must be valid numbers", BaseTransientBottomBar.LENGTH_SHORT)
            snackbar.setAction("Dismiss") { snackbar.dismiss() }
            snackbar.show()
        }

        if (name.isNotEmpty() && xCoord.isNotEmpty() && yCoord.isNotEmpty()) {
            lifecycleScope.launch {
                employeeDao.insert(EmployeeEntity(name = name, xCoordinate = xCoord, yCoordinate = yCoord))
                binding?.etName?.text?.clear()
                var snackbar = Snackbar.make(requireView(), "Pin $name saved", BaseTransientBottomBar.LENGTH_SHORT)
                snackbar.setAction("Dismiss") { snackbar.dismiss() }
                snackbar.show()
                hideKeyboard()
            }
        } else {
            var snackbar = Snackbar.make(requireView(), "Name or Coordinates cannot be blank", BaseTransientBottomBar.LENGTH_SHORT)
            snackbar.setAction("Dismiss") { snackbar.dismiss() }
            snackbar.show()
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
            val xCoord = binding.etUpdateXcoord.text.toString()
            val yCoord = binding.etUpdateYcoord.text.toString()

            try {
                xCoord.toDouble()
                yCoord.toDouble()
            } catch (e: java.lang.NumberFormatException) {
                var snackbar = Snackbar.make(requireView(), "X and Y Coordinates must be valid numbers", BaseTransientBottomBar.LENGTH_SHORT)
                snackbar.setAction("Dismiss") { snackbar.dismiss() }
                snackbar.show()
            }
            if (name.isNotEmpty() && xCoord.isNotEmpty() && yCoord.isNotEmpty()) {
                lifecycleScope.launch {
                    employeeDao.update(EmployeeEntity(id, name, xCoord, yCoord))
                    var snackbar = Snackbar.make(requireView(), "Pin $name updated", BaseTransientBottomBar.LENGTH_SHORT)
                    snackbar.setAction("Dismiss") { snackbar.dismiss() }
                    snackbar.show()
                    updateDialog.dismiss() // Dialog will be dismissed
                }
                hideKeyboard()
            } else {
                var snackbar = Snackbar.make(requireView(), "Name or Coordinates cannot be blank", BaseTransientBottomBar.LENGTH_SHORT)
                snackbar.setAction("Dismiss") { snackbar.dismiss() }
                snackbar.show()
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
        builder.setTitle("Delete Pin")
        //set message for alert dialog
        builder.setMessage("Are you sure you wants to delete pin ${employee.name}?")
        builder.setIcon(android.R.drawable.ic_dialog_alert)

        //performing positive action
        builder.setPositiveButton("Yes") { dialogInterface, _ ->
            lifecycleScope.launch {
                employeeDao.delete(EmployeeEntity(id))
                var snackbar = Snackbar.make(requireView(), "Pin ${employee.name} successfully deleted", BaseTransientBottomBar.LENGTH_SHORT)
                snackbar.setAction("Dismiss") { snackbar.dismiss() }
                snackbar.show()
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

    private fun hideKeyboard() {
        val imm: InputMethodManager =
            requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

}