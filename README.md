## I-Sight

## Youtube link
https://www.youtube.com/watch?v=jRx91pX4EXc

## Inspiration

Looking at how people treat each other, let aside the ones who really need help, humanity feels nothing more of a global scarcity. With the progressing technology there should be applications which help the people who really are in need and it would be better for people to atleast have technology which they can trust rather than being at nature's mercy. So we thought of building an application which would act as an in-person navigator and along with navigation also give out every detail a visually impaired person needs to know about his/her surrounding, keeping them safe.

## What it does

With our project I-Sight, we intend give these people with a disturbed vision a
trustworthy and reliable source to depend on when they walk out any place
which makes them feel endangered due to their disabilities. This application is in
itself a personal guide who’d rather hold one’s hand and walk them to wherever it
is they want to go. This application would act as one’s vision as well as a navigator
on the streets which would be far better than any human eyes could calculate or
predict. All this application requires is an android device with a camera.

## Features:

## COMPLETE NAVIGATION:

The user can use voice commands on the application and speak out the desired
location they want to reach, the application will then set a course from the users'
current location to the desired location and guide the user through the path
making use of various other features in the application.

## OBJECT DETECTION/RECOGNITION:

While moving the user has to keep the phone in his hands with the camera facing
in front, this application is running on an DL model which will detect and
recognize all the objects, moving and at rest, in the line of sight of the camera and
inform the user of all the objects which are present on his/her path.

## DISTANCE ESTIMATION:
This application will not only detect the objects in the vicinity but also estimate
the distance between them and the user and convert it into the number of steps
the user would require to get to any object present around the user.

## INTERMODAL ROUTING:
Along with guiding the user through the pedestrian route our app also has a feature 
of public transport guidance. If the path of the user is long the application automatically 
guides the user to the nearest public transport waypoint to help the user reach their 
destination, which also includes multiple changes in the transport journey if required.
Some of these public transport waypoints include city rail stations ,metro stations, bus stations etc.


## TIME TO COLLISION ALERT:
This application also provides a feature of alerting the user in order to prevent
any sorts of accidents or mishaps on the path of the user. This feature calculates
the time and the distance between the user and any obstruction or vehicle in it’s
on going path and alerts the user accordingly.


## How we built it

I-Sight is built using the power of Deep Learning models for object detection. It heavily relies on Tensorflow lite Mobile Net v1 model for fast, low latency and performant model and HERE SDK for the features of in-time navigation, Geolocation and the intermodal routing. For Voice Interaction it uses Android's Text To Speech API.

## Challenges we ran into

The implementation of tensorflow lite model in android as well as the implementation of HERE SDK required a bit of research. The merged output of both technologies was specifically challenging to achieve.

## Accomplishments that we're proud of

We are proud of being able to put together two such robust technologies in HERE SDK and Tensorflow and making them work together effortlessly. Along with that, we are also proud of providing our contribution to the society by helping visually disabled people to find there own way making them independent of others. 

## What I learned

We learned to implement light weight mobile nets and usage of tensorflow lite models. We also learned about the amazing HERE SDK which provides various robust features for geocoding, routing and LIVE Sense.

## What's next for I-Sight

- Another enhancement would be an addition of AI danger heuristic for safer travel.
