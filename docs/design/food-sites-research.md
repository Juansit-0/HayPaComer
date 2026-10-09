# Food sites research (plan v2, steps B4 to B6)

Real food and food-service sites reviewed in the browser on 2026-10-09 before redesigning the web interface. Each finding names what HayPaComer takes from it, so the redesign follows what people already use in Colombia instead of a generic template.

| Site | What it does | What we observed | What HayPaComer takes |
|---|---|---|---|
| SuperCook (supercook.com) | Recipes from what you have | The pantry is a left panel: a search box "add/remove/paste ingredients" on top, then groups by category ("Pantry Essentials 2/40", "Vegetables & Greens 0/100") with ingredient chips that turn green when you have them. A note says only salt, pepper, and water are assumed. Recipes are behind a sign-up | Adding food starts with a search field plus chips of common foods grouped by category; chosen chips are filled, not just outlined; each group shows a count |
| Rappi Colombia, Turbo market (rappi.com.co) | Grocery delivery | A category rail with Colombian aisle names: "Fruver", "Carnes y Pescados", "Lácteos y Huevos". Product cards put the price first in bold, the unit price in parentheses ("$25/g", "$8550/und"), then the name; a round "+" button sits on the card. Aisles ("pasillos") are listed on the left; shelves scroll sideways with "Ver más" | Colombian aisle names for categories; cards lead with the measurement (grams) and keep the food name second; a round add button on each card; grouping by aisle in the market list |
| Cookpad Colombia (cookpad.com/co) | Home recipes | Search results are rows: photo, a large title, the ingredients on one line separated by dots, then time and portions with small icons and a bookmark. A side filter reads "Muéstrame recetas sin" (show me recipes without) | "Cook now" results as rows with the title, minutes and people, and the ingredients on one line marking what is missing; diners' allergies already work as "recipes without" |
| Éxito (exito.com, fruits and vegetables) | Supermarket | Product names include the pack weight ("Malla 1000 gr"), the unit price follows in small text ("Gr a $ 6,00"), the price is large and orange, and a grid or list toggle sits above the results | Weights are always visible next to the food; unit prices in the market budget (B5) |
| Kitche (kitche.co) | Food waste app | The site is marketing with cartoon mascots; no product screens without installing | Not followed: HayPaComer stays a tool, not a toy |
| HelloFresh (hellofresh.com) | Meal kits | Blocked by a bot check that we do not bypass | Not reviewed |

## Decisions for B4

- **Add food is a sheet with steps:**
  1. find the food (search plus category chips);
  2. how much (grams stepper);
  3. where (shelf chips grouped by zone) and whether it is opened;
  4. when it expires, with three choices: "I know it" (date), "Estimate it" (shelf life rules), or "Photo of the label" (AI reads, Java checks).
- **Estimated dates are marked:** an estimated or AI date shows "≈" and the word estimated, so nobody trusts it like a printed date.
- **Shelf contents are cards:** grams first and large, then the food, the expiry chip, and actions ("Use", "Opened", "Throw away").
- **The cabinet stays the map of the fridge:** it is bigger, and each shelf shows its item count as a small badge.
