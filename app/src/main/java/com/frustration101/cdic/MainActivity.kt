package com.frustration101.cdic


import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

import android.app.AlertDialog
import android.view.View
import android.widget.ImageButton;





class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up Floating Action Button
        val fab = findViewById<FloatingActionButton>(R.id.fabCamera)
        fab.setOnClickListener {
            //Toast.makeText(this, "Camera button clicked!", Toast.LENGTH_SHORT).show()
			val intent = Intent(this, CameraActivity::class.java)
			startActivity(intent)
        }
		

    }

    // Inflate the menu into the ActionBar (toolbar)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_info, menu)
        return true
    }

    // Handle ActionBar item clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_info -> {
				showInfoDialog()
                //Toast.makeText(this, "Info clicked!", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
	

    private fun showInfoDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("About Coconut Disease Classifier")
        builder.setMessage(
            """
  🌴 Coconut Disease Image Classification App

  This app deploys a trained TensorFlow Lite model to classify coconut leaf images and detect common diseases affecting coconut trees. It has been developed as part of a thesis in partial fullfilment of the requirements for the Bachelor of Science in Computer Science college program.

  📱 Version: 2.0.1
  🛠️ Build: 2025.10.31

  👨‍💻 Developers:
     - Orocay, Rhea May A.
     - Gallaza, Angelica Mae B.
     - Garillo, Riza Mae S.

  © 2025 Thesis Group 5
            """.trimIndent()
        )
        builder.setPositiveButton("OK", null)
        builder.show()
    }

}