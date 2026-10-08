# RodeArena

Android Arena statistics app.

## Architecture
- Android client: Kotlin + Jetpack Compose
- Backend: FastAPI
- Statistics source: OP.GG Arena pages
- Riot game metadata/icons: Riot Data Dragon + CommunityDragon Arena metadata
- Client does not scrape OP.GG directly; the backend caches and normalizes data.

## Data rules
For a selected champion the API returns:
- top 10 Prismatic augments
- top 10 Gold augments
- top 10 Silver augments
- top 10 Arena Prismatic items
- top 10 normal/final items

Lists are ranked by player pick rate, not win rate. Each entry may include pick rate, game count and an official game icon.

## Important
The Android API URL must point to the deployed HTTPS backend. Do not ship OP.GG scraping credentials or private keys in the APK.

The GitHub Actions workflow builds a debug APK and publishes it as an artifact.
