# Adding Team Member Photos to About Section

## Overview

The about section now has placeholder images for all 4 team members. To add actual photos of you and your team members, follow these steps:

## Method 1: Add Photos to Drawable Folder (Recommended)

### Step 1: Prepare Your Photos

- Use square photos (e.g., 200x200 pixels or larger)
- Save them in JPG or PNG format
- Name them appropriately (e.g., `member1.jpg`, `member2.jpg`, etc.)

### Step 2: Add Photos to Project

1. Copy your photo files to: `app/src/main/res/drawable/`
2. Make sure the filenames are lowercase and contain only letters, numbers, and underscores

### Step 3: Update the Code

1. Open `app/src/main/java/com/example/mireve/AboutActivity.kt`
2. Find the `loadTeamMemberPhotos()` method
3. Update the resource names to match your actual photo files:

```kotlin
loadMemberPhoto(R.id.cardDev1, R.drawable.your_photo_name_1)
loadMemberPhoto(R.id.cardDev2, R.drawable.your_photo_name_2)
loadMemberPhoto(R.id.cardDev3, R.drawable.your_photo_name_3)
loadMemberPhoto(R.id.cardDev4, R.drawable.your_photo_name_4)
```

4. Uncomment this line in the `onCreate()` method:

```kotlin
loadTeamMemberPhotos()
```

## Method 2: Add Photos to Assets Folder

### Step 1: Create Assets Folder

1. Create folder: `app/src/main/assets/team_photos/`
2. Add your photos to this folder

### Step 2: Update the Code

1. In `AboutActivity.kt`, use the `loadMemberPhotoFromAssets()` method instead:

```kotlin
loadMemberPhotoFromAssets(R.id.cardDev1, "member1.jpg")
loadMemberPhotoFromAssets(R.id.cardDev2, "member2.jpg")
loadMemberPhotoFromAssets(R.id.cardDev3, "member3.jpg")
loadMemberPhotoFromAssets(R.id.cardDev4, "member4.jpg")
```

## Method 3: Direct XML Reference

You can also directly reference photos in the layout file:

1. Open `app/src/main/res/layout/activity_about.xml`
2. Find each team member's ImageView
3. Change the `android:src` attribute:

```xml
android:src="@drawable/your_photo_name"
```

## Current Placeholder Images

The app currently uses these placeholder images:

- Member 1: `@drawable/ic_member1` (Green)
- Member 2: `@drawable/ic_member2` (Blue)
- Member 3: `@drawable/ic_member3` (Orange)
- Member 4: `@drawable/ic_member4` (Purple)

## Tips for Best Results

1. **Photo Quality**: Use high-quality, well-lit photos
2. **Square Format**: Crop photos to square format for best appearance
3. **File Size**: Keep photos under 1MB for better app performance
4. **Naming**: Use descriptive names like `john_lead_developer.jpg`
5. **Consistency**: Use similar photo styles for all team members

## Troubleshooting

- If photos don't appear, check that the file names match exactly
- Make sure photos are in the correct folder
- Verify that the resource names in the code match your file names
- Check the Android Studio logcat for any error messages

## Example File Structure

```
app/src/main/res/drawable/
├── member1_photo.jpg
├── member2_photo.jpg
├── member3_photo.jpg
├── member4_photo.jpg
└── ... (other drawables)
```

Or for assets:

```
app/src/main/assets/team_photos/
├── member1.jpg
├── member2.jpg
├── member3.jpg
└── member4.jpg
```
