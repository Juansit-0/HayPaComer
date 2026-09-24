# Brand Story

## Version 1 - The long story (About page)

It is 6:40 p.m. and the question arrives, as it does every day: what can I cook right now with what is actually at home?

I open the fridge. There is a yogurt with no date, three bags of onions because everyone forgot we already had onions, someone else's cheese that is not mine to touch, and a leftover soup from Tuesday hiding behind the milk. I close the door and order delivery. Again.

That scene happened in a shared apartment, where groceries were a group project with no shared ledger. It happened in a family kitchen, where my mother measured waste in money quietly lost and dinners decided under pressure. And it bothered me as an engineer: billions of recipes online, cameras that guess ingredients, fridges with screens that cost more than a used car - and nobody solves the actual question, because nobody measures. The kitchen does not fail for lack of recipes. It fails for lack of evidence.

So I started with the smallest honest thing I could build: a scale that knows what is on the tray.

The first demo moment was almost funny. Milk went from 842 grams to 650. The system recorded 192 grams consumed, container weight already discounted. No typing, no discipline, no "remember to update the app". Just a fact that arrived by being weighed. From there the logic wrote itself: if a recipe asks for 200 grams of chicken and the tray measures 80, you reduce portions or you weigh a substitute. If the door stays open for 40 seconds, the buzzer and the household get told. If the power fails or the fridge runs warm, the cold rule says "under review" - because inventing a sanitary guarantee would be the one unforgivable lie.

I built it as a Java system with an ESP32 module and a cheap tray, because the homes that waste the most are not buying five-thousand-dollar appliances. I built it with rules in Java and AI as a removable helper, because safety should never depend on a language model, and dinner should not disappear when the WiFi does. And I built it with ownership and permissions as first-class ideas, because in a shared fridge, fairness is a feature.

This is for the people who buy groceries on Sunday and lose track by Wednesday. For the parent deciding dinner with two kids asking for food and no patience left. For the roommates negotiating whose cheese survives the week. You are the hero of this story; the system is just the honest instrument on the counter.

Here is the commitment I can be held to: grams are measured, never invented. Quantities, expiry, and cold chain are shown plainly, including when the answer is "I do not know". Rules decide safety, AI only helps with friction. The essentials - inventory, alerts, weighing, suggestions - work offline, always. And every claim about this product can be checked against a demo, a test, or a number you can weigh yourself.

Measurement beats memory. The fridge just needed to learn how to count.

## Version 2 - The short story (homepage or pitch)

At 6:40 p.m. the question arrives: what can I cook with what is actually at home? The fridge has three bags of onions, someone else's cheese, expired yogurt, and a soup nobody remembers. Delivery wins again.

I built HayPaComer because the kitchen does not fail for lack of recipes - it fails for lack of evidence. A scale under a tray changes that: 842 g becomes 650 g, and the system records 192 g consumed. A recipe asks for 200 g of chicken; the tray measures 80, so it offers to reduce or to weigh a substitute. When the door stays open or the fridge runs warm, rules - not models - decide what "under review" means.

This is for shared apartments and family homes that cook every day and hate paying twice for food. Affordable hardware, honest numbers, food with owners, and a system that keeps working when the internet does not. Measurement beats memory.

## Version 3 - The one-liner origin

HayPaComer started because I kept opening a full fridge at 6:40 p.m. with nothing to decide for dinner - and I believed that a tray that measures, not another camera that guesses, was the honest way to fix it.
