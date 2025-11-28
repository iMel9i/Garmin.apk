package iMel9i.garminhud.lite

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class LayoutEditorActivity : AppCompatActivity() {

    private lateinit var configManager: LayoutConfigManager
    private lateinit var currentProfile: LayoutProfile
    private var currentMode = "IDLE"

    private lateinit var slotMainNumber: TextView
    private lateinit var slotSecondaryNumber: TextView
    private lateinit var slotDirection: TextView
    private lateinit var slotLanes: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout_editor)

        configManager = LayoutConfigManager(this)

        setupViews()
        setupModeSpinner()
        loadProfile("IDLE")
    }

    private fun setupViews() {
        slotMainNumber = findViewById(R.id.slotMainNumber)
        slotSecondaryNumber = findViewById(R.id.slotSecondaryNumber)
        slotDirection = findViewById(R.id.slotDirection)
        slotLanes = findViewById(R.id.slotLanes)

        slotMainNumber.setOnClickListener { showSelector(HudSlot.MAIN_NUMBER) }
        slotSecondaryNumber.setOnClickListener { showSelector(HudSlot.SECONDARY_NUMBER) }
        slotDirection.setOnClickListener { showSelector(HudSlot.DIRECTION_ARROW) }
        slotLanes.setOnClickListener { showSelector(HudSlot.LANE_ASSIST) }

        findViewById<View>(R.id.btnSave).setOnClickListener {
            configManager.saveProfile(currentMode, currentProfile)
            Toast.makeText(this, "Профиль сохранен", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupModeSpinner() {
        val spinner = findViewById<Spinner>(R.id.spinnerMode)
        val modes = listOf("IDLE" to "Режим ожидания (Без навигации)", "YANDEX" to "Яндекс Карты / Навигатор", "GOOGLE" to "Google Maps")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modes.map { it.second })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedMode = modes[position].first
                if (selectedMode != currentMode) {
                    // Save previous before switching? Maybe auto-save or just discard?
                    // For simplicity, let's just load the new one.
                    currentMode = selectedMode
                    loadProfile(currentMode)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadProfile(mode: String) {
        currentProfile = configManager.getProfile(mode)
        updateUi()
    }

    private fun updateUi() {
        updateSlotUi(slotMainNumber, HudSlot.MAIN_NUMBER)
        updateSlotUi(slotSecondaryNumber, HudSlot.SECONDARY_NUMBER)
        updateSlotUi(slotDirection, HudSlot.DIRECTION_ARROW)
        updateSlotUi(slotLanes, HudSlot.LANE_ASSIST)
    }

    private fun updateSlotUi(view: TextView, slot: HudSlot) {
        val dataType = currentProfile.slots[slot] ?: HudDataType.NONE
        view.text = dataType.displayName
    }

    private fun showSelector(slot: HudSlot) {
        val options = HudDataType.values()
        val names = options.map { it.displayName }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Выберите данные")
            .setItems(names) { _, which ->
                val selected = options[which]
                currentProfile.slots[slot] = selected
                updateUi()
            }
            .show()
    }
}
