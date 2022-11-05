# Indoor Tracking (Microsleep)

## Table of Contents
1. [Problem Space](#problem-space)
2. [Intervention](#intervention)
3. [Feature List](#feature-list)
4. [Devices](#devices)
5. [Architecture](#architecture)
6. [Timeline](#timeline)

# Problem Space

***What is the problem you're addressing?***

People will not always know where something is. The cause could be due to being in an unfamiliar space or what they are looking for has moved. Their options are to find and ask someone who does know, use static navigation hints like signs or maps, or look around until they find it.

***Who is the target population/user?***

More focus on users in specific situations than a specific type of user.
* Users in target locations who need to find something but do not know where it is.

  * Limited access or insufficient assistance to resources that would be able to locate the object

    * Limited human assistance such as a store clerk or Airbnb host
    * Static environment navigation such as signs

* An example of target locations could be unfamiliar areas such as temporary housing (Airbnb, hotels), or a museum. Target locations could also be familiar places such as grocery stores.

* There’s potential for this to be applied to users that have trouble finding objects. Could be due to issues such as a disability or language barrier.

***Why is it important?***

* A lot of technology can get us to new places, but once we arrive there are many instances where navigation at the location is lacking. GPS may not be accurate enough or available indoors to guide users to what they are searching for.

* People might be frustrated when they can’t find what they are looking for, either in a familiar or an unfamiliar setting.

* Give users the ability to find objects in an unfamiliar location without someone else’s aid or become familiar with the location quicker.

***What previous work has been done in this space? Where did it succeed? Where did it fail?***

* Target App allows users to navigate through the store more easily by using the user’s GPS position and detecting where aisles are in the store and where certain items are so the user can navigate to them more easily

  * Success: When I use the app, I can easily find which aisle an item is on and have a general idea of how to get to the aisle and find the item

  * Failure	: GPS is not very granular indoors, which means it may only take me to the general area in the store, I still have to keep looking for it. In addition, aisles may be hard to find
  
* Apple Airtags/Tile: Allows users to open their phone and find where an object is by showing them the direction and distance they are from the tag.

    * Success: Users can know exactly what direction and distance the tag is, so that they can walk towards it and find their item

    * Failure: Requires physical tags, we plan on using virtual tags which are cheaper (doesn’t require any physical materials), and also would be resistant to movement (tags may be bumped away or fall off the item or taken away by pets/kids)

***Do your research on the problem, population, and related work. Give us any background information we need to understand the problem space. Show us that you understand the core of the problem and its complexities. Use the diverse backgrounds your teammates have; understand and deconstruct the problem from multiple perspectives.***

### Primary Journal

* [Advanced Indoor and Outdoor Navigation System for Blind People Using Raspberry-Pi](https://jit.ndhu.edu.tw/article/viewFile/2234/2247)

This paper uses computer vision techniques to detect objects. First, the camera captures the video into the system. The pixels that do not pass the certain threshold will be considered as ground. After that, the non-ground object still needs to go through SURF(speeded up robust features) and PDF (probability thickness work) to do the final recognition. 

RaspberryPi is used as the interface here. After the system recognizes the object, the researchers use Google Voice Recognizer API to transfer text to speech. Below is the main architecture from the paper:

![architecture diagram](https://i.imgur.com/UhI5KjK.png)

Compared with our method, we’re not going to do fancy computer vision techniques to detect objects. We’re detecting the phone on the user to calculate the distance. And for the voice speaker, this paper gives us an idea of instead using Amazon Alexa, maybe we can have another approach such as Google Voice Recognizer.

### Secondary Journal

* [Brains of blind people adapt to sharpen sense of hearing, study shows](https://www.washington.edu/news/2019/04/22/brains-of-blind-people-adapt-to-sharpen-sense-of-hearing-study-shows/)

The article points out that people with visual impairment have better hearing than normal vision people. Losing sight makes their hearing become keener than normal vision people. This article gives us the inspiration of helping the visually impaired as our next step to combine more features with voice assistance. This backups our idea of integrating voice assistance into our project. Normal vision people usually can find the position of the object easily by their sight. However, people with visual impairment rely more heavily on their hearing. Therefore, we believe our project can improve their quality of life significantly.

### Supplementary Journals
* [Bluetooth localization](https://ieeexplore.ieee.org/abstract/document/4394215)
* [Mapping room using bluetooth/ultrasound](https://dl.acm.org/doi/abs/10.1145/2809695.2809727)
* [Indoor tracking using WiFi signal](https://dl.acm.org/doi/abs/10.1145/2988287.2989142)

# Intervention

***How do you plan to address the problem/part of the problem?***

* We’re going to use an agile approach to develop our application. We’ll go through the sprint each week. Accomplish weekly targets and push the progress each week. We go through this iteration for five weeks and make sure we have time to refactor and get the feedback.

* We want to create an app to aid with the tracking of indoor objects and navigation to those objects.
  * Will be done by having wireless sensors around the indoor space to triangulate positions of the user and objects.
    * Can also look into using other mediums to improve accuracy of tracking such as sound or infrared light sensors.
  * Should give a better experience to find a specific indoor object or location similar to navigation apps such as Google Maps.
* Version 2 A local map of the area around you. Can filter for specific objects or show all objects.

***Why do you think it will help give your problem space (related work, target population, etc.)?***

* Provides assistance to find objects without another person’s help. Could be no one is available or other issues that prevent communication.
  * Airbnb power users may have a difficult time communicating with the host. Our app can mitigate some problems by reducing these situations where guests can identify what they need without host assistance.
* Lower discovery time, improve task efficiency, improve ease of use of (semi-)public areas, potential for PwMCI (people with mild cognitive impairment)
* As mentioned above, people need time to figure out the environment when they come to an unfamiliar space. With assistance from our application, users can quickly get familiar with the environment and figure out where the potential items they need are in the space.

***Give us your vision of what your intervention/solution will do and how it will do it. Walk us through what using it will be and feel like. Add pictures and drawings if helpful. Convince us that this is the right solution to your problem.***

* Use sensors of some kind (wireless signal and measure signal strength) to triangulate a user’s position. From there it should feel like a mobile navigation app where it directs you in the direction of the objects.
  * Multiple wireless sensors and a wifi network around a space should give us a position we can use to direct users to the target position.
    * Estimotes can provide the user's location within the area the sensors are tracking through their [API](https://github.com/Estimote/Android-Indoor-SDK) 
  * GPS may not be available indoors or provide the precision that would be required.
  * Other sensors could give us more information about the users position/orientation. 
    * Sensors in the device such as compass/Android geolocation API could potentially give us users orientation and not just position.
      * Potential magnetic interference
    * Infrared light could be used as a line of sight check. 
      * Accounts for signals being blocked by walls but still readable.
      * Alternative could be using ultrasound and microphone. 
      * Will require more research about what is available and feasible for us to implement in this time.

***Consider that you will be demoing this at the end of the quarter. How will you demonstrate your intervention?***

Will have two demonstrations:

* First, a general user goes inside the room and sees how quick they can find the target object.

* Second, a general user with our app can search for an item where we can record if time spent on items found decreases.

# Feature List

***Give us a breakdown of the features you intend to implement with your system, what it will do, why it exists, and an example of how someone might use it (a storyboard). You may find drawings useful here to describe user interactions and interfaces.***

* **Features**

  * *Allow a user to prepare the area by creating a list of pinned places or items in the target location.*
  * Ex. A grocery store can create pins in each aisle. As inventory comes in, they can map their wares to a pin they have created.
  * Nice to have: Allow these users to share these lists with other users with an optional duration.
    * Ex. An Airbnb host can share the layout of the space they are renting out for the duration of the stay.
  * Nice to have: Allow these users to connect multiple pins together to mark obstacles/walls or regions like a room.
    * Ex. Mark walls for better navigation in rooms that have obstacles such as couches, or non uniform rooms with angled or protruding walls.

*Users who are unfamiliar with the layout of the area can then be guided toward pinned marks.*

* Ex 1. User is grocery shopping and wants to try a new recipe. They need an ingredient they have never used before and do not know where it is. They can use the app to guide them to the correct aisle and location in that aisle where the ingredient is.
* Ex 2. Ben is looking for Harry Potter in the library. He does not know where it is or what section of the library to look for. Our app would guide Ben to the correct floor and section of the library to find the book he is searching for without needing to find someone who knows where it is.
* Ex 3. Ben is cooking in an unfamiliar kitchen. He does not know where common tools are such as cooking utensils, cookware, or safety items such as a fire extinguisher.

# Devices

***What devices will you need to implement your intervention? You can use devices that we covered in class, but you are welcome to go beyond those in class if you have access to them. How do you plan to power the devices?***

* Sensors as beacons to get the users position
  * Estimote - use to detect the user location inside the room
    * Resource: Video Link, Estimote GitHub
  * Or Raspi for Bluetooth/WiFi - Detect the user distance of the object
    * Sensors:
      * Ultrasonic or infrared to detect distance
      * Camera (Optional) recognize the object
  * Device to present info to user and get users position/orientation
    * Phone/wearable - Connect with Estimote and use vibration to notify the users
    * Voice activated navigation - Alexa (nice to have) - Use voice assistance to guide the user in indoor navigation

# Architecture

***Are you using a network?***
* Demo: No
* Final: Yes

***Where is your server? What devices are clients?***
* Demo: will store data locally
* Clients (phone/wearable) will connect to sensors around the room to triangulate their position
* Final: 
  * Server will be a cloud server.
    * Holds account data, layouts, sensors
  * The client is a user’s phone or wearable
    * Fetches accounts, layouts and sensors upon app login

***How is data stored?***
* Demo: locally on device
    * sensors (position and metadata about sensor)
    * pin information (names, locations)
  * Final: On a remote server
***Are you doing anything to protect user information?***
* Demo: All data should be stored on the device and there shouldn’t be any sensitive data sent anywhere else.
* Final:
  * Personal information (email, name) should not normally be shown to other users unless shared. Passwords would be hashed for security.
  * As an extra, if GPS is available, could potentially use fine location in tandem with indoor sensors. That would be done through the app permissions and probably not need to be stored on the server.
  * Extras: we want to have an optional time duration for how long the shared pin location will be available to the other user.

***Create a diagram or series of diagrams showing the flow of information through your system. Show how devices are connected and what protocols they use. Describe how information on each device is handled, whether it is saved, sent, or discarded. Give us a graphic of your system/network architecture. Don't forget to show the user where the data is relative to the user***

### App Logic

**Objects are Pinned:**

<img src="https://i.imgur.com/CzHLYzJ.png" alt="drawing" width="600"/>

 
**Objects are Located Upon Request**

<img src="https://i.imgur.com/8yAe8gu.png" alt="drawing" width="600"/>

Here, the diagram shows that objects will be pinned and will exist as an object with atributes. Attributes include name, coordinates, etc. Based on this information, the phone will be able to compare its coordiantes in real time with the pin objects. Once the two are overlating one another, the program will alert the user that they are in the correct position.

***We will cover this topic in class on 11/1/22 and don't expect you to know what protocols to use where, but do your best, it's okay to be a little vague, but try to look up what protocols will be best for your purposes.***

* Server will most likely have an HTTP API to communicate with clients. It will  handle server side functions such as creating accounts, and storing/creating/deleting layouts, sensors, and pins. Will also provide layouts/shared layouts and their sensors/pinned objects when they login.

* Client devices would communicate with wireless sensors in locations to determine their location. Maybe done with the signal strength.
  * Could also look into light (distance, line of sight) or sound (loudness to measure distance and potentially line of sight)
 
## User Interface

**Home Screen**

<img src="https://i.imgur.com/5sdvtLN.png" alt="drawing" width="300"/>

**Find Screen**

<img src="https://i.imgur.com/dy7pw7f.png" alt="drawing" width="300"/>

Here, in the diagram you can see our idea of an MVP interface. The homescreen contains three essential buttons; Find, Pin, My Items. Find will take you to the Find Screen whre you can request to locate objects. Pin will take you to a screen similar to the Find Screen, but you'll be able to create a new object and pin it. My Items is a list of all pinned items that are saved.

In the Find Screen, note that a user can tap on any object on the screen to have guidance to the requested object. A user can also search for an object or select an object based on category.

# Timeline

***You have 4-5 weeks. Give us a weekly breakdown of what features you intend to implement for that week. Keep in mind that the best plans are adaptable and give yourself some buffer room in case you fall behind schedule; have extra/bonus features planned if you find yourself with extra time. Distribute the work between your teammates. Don't forget to leave room for testing and make everything stable for the final demo.***


We’re going to use an agile approach to develop our application. We’ll go through the sprint each week. Accomplish weekly targets and push the progress each week. We go through this iteration for five weeks and make sure we have time to refactor and get the feedback.

## Week 6

**Milestone:**  Get sensors setup to detect users' devices.

* Divide work to people, decide who will do what: 1-2 people on hardware, 1-2 on backend, 1 on frontend? or be more flexible and we don’t have a set task to work on.
* Hardware
  * Get sensors to detect user
  * research/find (or implement) a way to turn that into a position.
* Frontend
  * Plan the user UI, get a basic testing UI

## Week 7
**Milestone:** Calculate a users position from the sensors, maybe get UI showing absolute positions of user and pins
* Hardware
  * Turn signal strength(?) into users position
  * Detect user orientation/Implement directions to a pin from a location
* Frontend
  * Design planned UI on Figma 

## Week 8
**Milestone:** Calculate user orientation (direction they are facing), get UI to take user orientation into account and update the display accordingly
* Hardware
  * Implement directing user to a pin based on their current location/orientation
* Frontend
  * Start creating earlier designs in an app
  * Continue designing on Figma 

## Week 9
**Milestone:** Direct user in the correct direction to the marked object, make a UI that can show this and direct the user to the object.
* Hardware
  * Maybe another week for Implement directing user to a pin based on their current location/orientation 
* Frontend
  * Finish/finalize design on UI we designed
* Start putting together the final report and presentation.

## Week 10
**Milestone:** Extra week as time buffer for previous weeks. Polish final demo,maybe add a cleaner UI, try to find/fix any bugs. Work on final demo and final report.
* Hardware
  * Combine everything together and fix bugs and polish anything else that remains?
  * Add a voice assistant version if there’s time?
* Frontend
  * Finish/finalize design on UI we designed
* Finish working on final report/demo
