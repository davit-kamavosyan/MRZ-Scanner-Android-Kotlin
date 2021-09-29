/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package davit.kamavosyan.mrzscanner.scanner

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.ln

/** Processor for the text detector demo.  */
class TextRecognitionProcessor(private val context: Context,private val listener:CameraEventListener, textRecognizerOptions: TextRecognizerOptionsInterface) : VisionProcessorBase<Text>(context) {
  private val textRecognizer: TextRecognizer = TextRecognition.getClient(textRecognizerOptions)
  private val shouldGroupRecognizedTextInBlocks: Boolean = PreferenceUtils.shouldGroupRecognizedTextInBlocks(context)

  override fun stop() {
    super.stop()
    textRecognizer.close()
  }

  override fun detectInImage(image: InputImage): Task<Text> {
    return textRecognizer.process(image)
  }

  override fun onSuccess(text: Text, graphicOverlay: GraphicOverlay) {
    Log.d(TAG, "On-device Text detection successful")




    val filteredTextBlocks = text.textBlocks.filter { it.text.length == 44 && it.text.contains("<")}

    val filteredTextBlocksIDCard = text.textBlocks.filter { it.text.length == 30 && it.text.contains("<")}

    if(filteredTextBlocksIDCard.size == 3){

      CoroutineScope(Dispatchers.Main).launch {

        try {
          var line1 = filteredTextBlocksIDCard.get(0).text
          var line2 = filteredTextBlocksIDCard.get(1).text
          var line3 = filteredTextBlocksIDCard.get(2).text

          line1 = line1.replace(" ","")
          line2 = line2.replace(" ","")
          line3 = line3.replace(" ","")

          val nameData = line3.split("<").filter { it.length > 1 }
          var lastname = nameData[0]
          var firstName = nameData[1]

          lastname = lastname.replace(" ","")
          firstName = firstName.replace(" ","")

          var passportId = line1.subSequence(5,14).toString()
          passportId = passportId.replace("o","0")
          passportId = passportId.replace("O","0")
          passportId = passportId.replace(" ","")

          listener.onIdCartDetected(fName = firstName, lName = lastname,passportId = passportId)

        }catch (e:java.lang.Exception){

        }
      }


    }


    //check for passport
    if(filteredTextBlocks.size == 2){

      CoroutineScope(Dispatchers.Main).launch {

        try {
          var line1 = filteredTextBlocks.get(0).text
          var line2 = filteredTextBlocks.get(1).text

          line1 = line1.replace(" ","")
          line2 = line2.replace(" ","")

          val namePart = line1.substring(5)
          val nameData = namePart.split("<").filter { it.length > 1 }
          var lastname = nameData[0]
          var firstName = nameData[1]

          lastname = lastname.replace(" ","")
          firstName = firstName.replace(" ","")

          val passportCode = line2.subSequence(0,2).toString()
          val passportNumber = line2.subSequence(2,9).toString()

          var newNumber = passportNumber.replace("o","0")
          newNumber = newNumber.replace("O","0")
          newNumber = newNumber.replace(" ","")

          listener.onPassportDetected(fName = firstName, lName = lastname,passportId = "$passportCode$newNumber")

        }catch (e:java.lang.Exception){

        }
      }
    }

    if(filteredTextBlocks.size > 1){
      val filteredText = Text(text.text,filteredTextBlocks)
      logExtrasForTesting(filteredText)
      graphicOverlay.add(TextGraphic(graphicOverlay, filteredText, shouldGroupRecognizedTextInBlocks))
    }

    if(filteredTextBlocksIDCard.size > 1){
      val filteredText = Text(text.text,filteredTextBlocksIDCard)
      logExtrasForTesting(filteredText)
      graphicOverlay.add(TextGraphic(graphicOverlay, filteredText, shouldGroupRecognizedTextInBlocks))
    }
  }

  override fun onFailure(e: Exception) {
    Log.w(TAG, "Text detection failed.$e")
  }

  companion object {
    private const val TAG = "TextRecProcessor"
    private fun logExtrasForTesting(text: Text?) {
      if (text != null) {
        Log.v(
          MANUAL_TESTING_LOG,
          "Detected text has : " + text.textBlocks.size + " blocks"
        )
        for (i in text.textBlocks.indices) {
          val lines = text.textBlocks[i].lines
          Log.v(
            MANUAL_TESTING_LOG,
            String.format("Detected text block %d has %d lines", i, lines.size)
          )
          for (j in lines.indices) {
            val elements =
              lines[j].elements
            Log.v(
              MANUAL_TESTING_LOG,
              String.format("Detected text line %d has %d elements", j, elements.size)
            )
            for (k in elements.indices) {
              val element = elements[k]
              Log.v(
                MANUAL_TESTING_LOG,
                String.format("Detected text element %d says: %s", k, element.text)
              )
              Log.v(
                MANUAL_TESTING_LOG,
                String.format(
                  "Detected text element %d has a bounding box: %s",
                  k, element.boundingBox!!.flattenToString()
                )
              )
              Log.v(
                MANUAL_TESTING_LOG,
                String.format(
                  "Expected corner point size is 4, get %d", element.cornerPoints!!.size
                )
              )
              for (point in element.cornerPoints!!) {
                Log.v(
                  MANUAL_TESTING_LOG,
                  String.format(
                    "Corner point for element %d is located at: x - %d, y = %d",
                    k, point.x, point.y
                  )
                )
              }
            }
          }
        }
      }
    }
  }

  interface CameraEventListener{
    fun onPassportDetected(fName:String,lName:String,passportId:String)
    fun onIdCartDetected(fName:String,lName:String,passportId:String)

  }

}

