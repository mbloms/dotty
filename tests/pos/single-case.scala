object test where

  try
    println("hi")
  catch case ex: java.io.IOException => println("ho")


