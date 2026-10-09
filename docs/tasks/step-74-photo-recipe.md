# Step 74: Photo to structured, verifiable recipe

Commit and pull request title: `feat(ai): photo to structured, verifiable recipe`

## Goal

A household can photograph a cookbook page or a handwritten card and get a recipe draft. The AI only reads the photo; Java checks every ingredient against the food catalog and converts every quantity to grams. Nothing is saved: the person reviews the draft and saves it with the existing recipe endpoint.

## Scope

- Application `ai`:
  - `RecipePhoto` (JPEG, PNG, or WebP only, up to 4 MB, magic bytes must match the declared type, defensive copies);
  - port `RecipePhotoReader` returning `PhotoRecipe` with `PhotoIngredient`s;
  - `ReadRecipePhoto` (members only, shares the 20 calls per minute AI limit, audits VALID or UNAVAILABLE in `AiAuditLog`) builds a `RecipeDraft` whose `DraftIngredient`s carry the catalog food, the grams from `QuantityParser`, or the problem ("Not in the food catalog", "Quantity cannot be weighed");
  - `PhotoReadingUnavailableException`.
- `adapter-ai`:
  - `LlmPrompt` takes an optional `LlmImage` (base64, never printed);
  - Gemini sends it as `inline_data` and OpenAI-compatible providers as an `image_url` data URL;
  - `LlmRecipePhotoReader` enforces a strict JSON contract (name 80, servings 1-50, minutes 1-1440, 1-30 ingredients, up to 30 steps of 300 characters, confidence 0-1);
  - `OfflineRecipePhotoReader` explains that photos need an AI provider.
- Web:
  - `POST /households/{h}/recipes/from-photo` (multipart `photo`) answers the draft with `verified` and `saved: false`;
  - 400 for a wrong type or content, 413 over 4 MB, 503 when no provider can read it;
  - `AiConfiguration` picks the reader from `AI_PROVIDER`.

## Tests (definition of done)

- `ReadRecipePhotoTest`: item-by-item verification, a fully verified draft, outage audit, strangers, rate limit, photo validation.
- `LlmRecipePhotoReaderTest` and `HttpClientsTest`: contract enforcement, the image in both provider formats, outages, offline mode.
- `RecipePhotoIntegrationTest`: draft never saved, wrong content 400, stranger 404, anonymous 401, provider down 503.
