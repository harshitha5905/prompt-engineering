package com.example.m2c1

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.m2c1.databinding.ActivityMainBinding
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * MainActivity demonstrating Prompt Engineering techniques with a modern UI.
 *
 * Techniques:
 * 1. Zero-Shot: Raw user input.
 * 2. Few-Shot: Input wrapped in a few examples to set context and format.
 * 3. Chain-of-Thought (CoT): Instruction to think step-by-step for logical depth.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    
    // Retrieve API key from local.properties via BuildConfig
    private val apiKey = BuildConfig.API_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.btnGenerate.setOnClickListener {
            val input = binding.etInput.text?.toString()?.trim() ?: ""
            if (input.isBlank()) {
                Toast.makeText(this, "Please enter a prompt", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (apiKey == "YOUR_API_KEY_HERE" || apiKey.isEmpty()) {
                Toast.makeText(this, "Missing API Key in local.properties", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            hideKeyboard()
            generateResponses(input)
        }
    }

    private fun generateResponses(userInput: String) {
        val model = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )

        // UI State: Loading
        binding.progressBar.visibility = View.VISIBLE
        binding.btnGenerate.isEnabled = false
        resetOutputs()

        lifecycleScope.launch {
            try {
                // Technique 1: Zero-Shot
                val zeroShotPrompt = userInput

                // Technique 2: Few-Shot (Instructional examples)
                val fewShotPrompt = """
                    Task: Provide a concise and accurate answer.
                    Example 1: Q: Is Pluto a planet? A: No, it is classified as a dwarf planet.
                    Example 2: Q: Who wrote '1984'? A: George Orwell.
                    Q: $userInput
                    A:
                """.trimIndent()

                // Technique 3: Chain-of-Thought (Reasoning)
                val cotPrompt = "$userInput\n\nThink through this step-by-step and provide a detailed explanation before the conclusion."

                // Parallel Execution
                val zeroShotDef = async { model.generateContent(zeroShotPrompt) }
                val fewShotDef = async { model.generateContent(fewShotPrompt) }
                val cotDef = async { model.generateContent(cotPrompt) }

                val zRes = zeroShotDef.await()
                val fRes = fewShotDef.await()
                val cRes = cotDef.await()

                // Update UI
                binding.tvZeroShot.text = zRes.text ?: "No data"
                binding.tvFewShot.text = fRes.text ?: "No data"
                binding.tvChainOfThought.text = cRes.text ?: "No data"

                updateScorecard()

            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                binding.tvZeroShot.text = "Error occurred."
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnGenerate.isEnabled = true
            }
        }
    }

    private fun resetOutputs() {
        binding.tvZeroShot.text = "Generating..."
        binding.tvFewShot.text = "Generating..."
        binding.tvChainOfThought.text = "Generating..."
        binding.tvComparisonQuality.text = "---"
        binding.tvComparisonClarity.text = "---"
        binding.tvComparisonAccuracy.text = "---"
    }

    private fun updateScorecard() {
        binding.tvComparisonQuality.text = "High (CoT offers most detail)"
        binding.tvComparisonClarity.text = "Excellent (Few-Shot is formatted)"
        binding.tvComparisonAccuracy.text = "Superior (CoT reduces logical errors)"
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
