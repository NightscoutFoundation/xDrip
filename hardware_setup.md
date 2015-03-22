## What you need to get started
Note, the following describes what I am currently using and recommend, it could definitely change and you can definitely use other components. Just know that this is all that it has been tested with!

* An Android Device
 * Must be running 4.3 to support BLE
 * Also must have BLE support
* [A Wixel](http://www.pololu.com/product/1337)
 * I recommend the one without headers(linked) if you want to use wires, if you plan to use a breadboard or make a custom PCB feel free to get the one with headers.
* An HM-10 BLE Module
 * I removed the link as it seems they replaced the HM10 with a cc41, **DO NOT GET A CC41!!!!!** I use the hm10 formerly sold by sunfounder, if you can find one with the pins at the bottom that is the easiest!
 * The HM11 has been confirmed working but soldering it is VERY TOUGH
* [Battery Power](http://www.adafruit.com/products/258)
 * I use the one linked and have no problem getting through a day, I have not done extensive battery testing at this point
 * *EDIT: that one was lasting over 6 days, which seemed like a waste of space, now I use [this 500mAh](http://www.adafruit.com/products/1578) Which gets me two days no problem!
* [Battery Charger](http://www.adafruit.com/products/1904)
 * I use this one, its nice and smallish and does the trick.
 * If you find a nicer smaller one PLEASE LET ME KNOW (I wound up breaking the top bit off of this so I could make it fit in a new enclosure)
* [Wires](http://www.adafruit.com/product/2051)
 * I use these 30AWG silicon wires because they are super small and flexible
 * Larger wires will make it harder to keep things nice and compact, keep with a small guage!
* Solder and Soldering Iron
 * Or a breadboard if you dont mind the bulk
 * Or a custom made PCB (and one for me too please?)
 * I used a cheap 15w soldering iron in order to solder and remove the pins from the HM10 (you can probably go a bit higher but I wouldnt go crazy)
 * I used a regular not terrifying soldering iron for the rest of it!
* [Resistors](http://www.radioshack.com)
 * If you wish to measure the Wixel's battery voltage, you'll need to build a voltage divider using two resistors and a piece of wire
 * You'll need a 1/4w 1K Ohm and a 1/4w 2.2K Ohm resistor.  1/8w should work, and be smaller, but it hasn't been tested
 * Optional:  Small heatshrink tubing to protect the resistors (https://www.adafruit.com/product/344)

 ## Putting it together!
 ![SETUP](http://i.imgur.com/EIGki5R.png)
 
 ## For the voltage measurement modification
 ###### If you have already built the xDrip and are adding this, follow this first step:
  * Remove the solder from the VIN and GND pins on the Wixel if possible using a solder sucker or solder wick.  If this is not possible, then you will have to work carefully by keeping the holes heated while inserting the resistors.  Keep in mind, if you do not have a controllable temperature soldering iron, you risk burning the board or the pads and ruining the Wixel.  **I highly recommend getting a solder sucker.**
  * You will need to grab a new copy of the Wixel code and upload it to your Wixel for this modification to work.  The process is the same as when you first loaded the code onto the Wixel, except you will need to click the "Erase Flash" button first.

 ###### Building the voltage divider
 * Bend the one end of each resistor at a 90 degree angle as close to the resistor as possible 
 ![Imgur](http://i.imgur.com/TXPCYx9.jpg)
 * Using helping hands or tape, face the two resistors towards each other and place them side by side so that the leads are touching at the top
 ![Imgur](http://i.imgur.com/WJNnoBv.jpg)
 * Solder the touching leads, and then solder on the piece of wire
 ![Imgur](http://i.imgur.com/jLdzr6q.jpg)
 ![Imgur](http://i.imgur.com/W3praDM.jpg)
 * Trim the leads of the 90 degree bend so they are flush with the resistors and trim any excess exposed wire from the piece you added to prevent any possible shorts.
 ![Imgur](http://i.imgur.com/IzpXo1L.jpg)
 * Insert the leads of the voltage divider into the VIN hole and GND hole.  The 1K resistor goes into the VIN hole and the 2.2K resistor goes into the GND hole next to it.  Additionally, insert the wires from the LiPo charger into the appropriate VIN and GND holes.
 ![Imgur](http://i.imgur.com/zpLtRoc.jpg)
 * CAREFULLY bend the resistor leads you inserted so the voltage divider is flat against the Wixel
 ![Imgur](http://i.imgur.com/cvRmHaO.jpg)
 * Solder the VIN and GND pads
 ![Imgur](http://i.imgur.com/N9mqE99.jpg)
 * Trim the excess wire from the resistor leads
 ![Imgur](http://i.imgur.com/EFCnUlv.jpg)
 * The back of your Wixel should look like this
 ![Imgur](http://i.imgur.com/eYh9yOR.jpg)
 * If you want to add a piece of heat shrink tubing to protect the resistors (highly recommended!), put it on now.
 ![Imgur](http://i.imgur.com/4vyTY4v.jpg)
  * Trim the wire you added to the voltage divider so that it is long enough to reach P0_0, and then insert it into the hole and solder it.
 ![Imgur](http://i.imgur.com/4b625P5.jpg)
 ![Imgur](http://i.imgur.com/97ZKzcC.jpg)
 * Trim the excess leads from the soldered pads on the Wixel
 ![Imgur](http://i.imgur.com/fXgbnp5.jpg)
 * CAREFULLY heat the heat shrink tubing with the iron.  Only touch the tubing for a second to get it to slightly heat up and do this in multiple places.  You only want it to shrink enough to not fall off, it does not have to be super tight against the resistors.  **DO NOT USE A LIGHTER!**
 ![Imgur](http://i.imgur.com/KfWcZfd.jpg)
 * Congratulate yourself for building an electronic circuit!
I also tossed it all into a "Crush Proof Pill Box" from CVS (Cost like $2)
![Imgur](http://i.imgur.com/uB40JUG.jpg)
![Imgur](http://i.imgur.com/8xIdz5w.jpg)
