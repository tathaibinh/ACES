package manojromina.aces;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;


public class EditPostActivity extends AppCompatActivity {
    private String mPost_key = null;
    private ImageView mEditImageView;
    private TextView mEditTitleField;
    private TextView mEditDescriptionField;
    private Button mEditButton;



    //when user select the image then it will have some value till then the value is null.
    private Uri mImageUri =  null ;

    //Storage Reference;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStorageReference;

    //Firebase Database
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private DatabaseReference mDatabaseUser;

    //firebase auth
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mCurrentUser;

    //progress bar
    private ProgressDialog mProgress;
    private static final int GALLERY_REQUEST = 1;

    private String post_image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        mProgress = new ProgressDialog(this);

        //firebase auth
        mFirebaseAuth = FirebaseAuth.getInstance();

        mCurrentUser = mFirebaseAuth.getCurrentUser();

        //getting instance of firebase storage from a particular app
        mFirebaseStorage = FirebaseStorage.getInstance();
        //creating a reference this will connect the app with the root directory of the firebase storage
        mStorageReference = mFirebaseStorage.getReference();


        //firebase datbase
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        //mDatabaseReference = mFirebaseDatabase.getReference(); This will store the data in root but we dont want to
        // store data in the root
        //so we created a child in the database.
        mDatabaseReference = mFirebaseDatabase.getReference().child("Blogs");

        mDatabaseUser = mFirebaseDatabase.getReference().child("Users").child(mCurrentUser.getUid());

        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Blogs");
        mFirebaseAuth = FirebaseAuth.getInstance();

        mPost_key = getIntent().getExtras().getString("edit_blog_id");
        System.out.println("Post Key is " + mPost_key);


        mEditImageView = (ImageView) findViewById(R.id.editImageView);
        mEditTitleField= (TextView) findViewById(R.id.editTitleField);
        mEditDescriptionField = (TextView) findViewById(R.id.editDescriptionField);
        mEditButton = (Button) findViewById(R.id.editSubmitButton);


        // this method is use to get the blog elements
        mDatabaseReference.child(mPost_key).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String post_title = (String) dataSnapshot.child("title").getValue();
                String post_description = (String) dataSnapshot.child("description").getValue();
                 post_image = (String) dataSnapshot.child("image").getValue();



                mEditTitleField.setText(post_title);
                mEditDescriptionField.setText(post_description);


                Glide.with(EditPostActivity.this)
                        .load(post_image)
                        .into(mEditImageView);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        //when submit button is pressed
        mEditButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                startPosting();
            }
        });


        //when image button is click then the gallery should be opened
        mEditImageView.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent,GALLERY_REQUEST);
            }
        });
    }

    //if the image is been slected than to retrieve that image

    //when the image from the gallery is selected and the image is cropped
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GALLERY_REQUEST  && resultCode == RESULT_OK)
        {

            mImageUri = data.getData();
            CropImage.activity(mImageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1,1)
                    .start(this);

        }

        //when image is crop then changing the default pic to the cropped image pic
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                mEditImageView.setImageURI(resultUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
    private void startPosting()
    {
        mProgress.setMessage("Posting Blog..");


        final String title = mEditTitleField.getText().toString();
        final String description = mEditDescriptionField.getText().toString();
        //if the title,description and image is enteres then only blog can be posted.
        if(!TextUtils.isEmpty(title) && !TextUtils.isEmpty(description) && !TextUtils.isEmpty(post_image))
        {
            mProgress.show();
            //this method will return the last path segment. that is the number
            //suppose we have blog/5 image
            //so it will return 5

            //if image has been changed
            if(mImageUri != null) {
                StorageReference filepath = mStorageReference.child("Blog_Images").child(mImageUri.getLastPathSegment());

                //upload file to firebase storage
                filepath.putFile(mImageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        // When the image has successfully uploaded, we get its download URL
                        final Uri downloadUrl = taskSnapshot.getDownloadUrl();

                        //push creates a unique key for each new post
                        final DatabaseReference newPost = mDatabaseReference.child(mPost_key);

                        //if we are able to retrive the name of the user then only we should post the detail
                        mDatabaseUser.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {


                                //mDatabaseReference was pointing to the Blogs
                                //now the new post will create three childs in Blogs
                                //Title,Description,Image
                                newPost.child("title").setValue(title);
                                newPost.child("description").setValue(description);
                                newPost.child("image").setValue(downloadUrl.toString());
                                newPost.child("uid").setValue(mCurrentUser.getUid());
                                newPost.child("username").setValue(dataSnapshot.child("name").getValue()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if (task.isSuccessful()) {
                                            //when the post is done the mainactivity will be open
                                            startActivity(new Intent(EditPostActivity.this, MainActivity.class));

                                        }

                                    }
                                });


                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                        //when the image is successfully uploaded at that time stop the progressbar
                        mProgress.dismiss();


                    }
                });
            }

            //if image is same but the content are change
            else
            {
                //push creates a unique key for each new post
                final DatabaseReference newPost = mDatabaseReference.child(mPost_key);


                //if we are able to retrive the name of the user then only we should post the detail
                mDatabaseUser.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {


                        //mDatabaseReference was pointing to the Blogs
                        //now the new post will create three childs in Blogs
                        //Title,Description,Image
                        newPost.child("title").setValue(title);
                        newPost.child("description").setValue(description);
                        newPost.child("image").setValue(post_image.toString());
                        newPost.child("uid").setValue(mCurrentUser.getUid());
                        newPost.child("username").setValue(dataSnapshot.child("name").getValue()).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                if (task.isSuccessful()) {
                                    //when the post is done the mainactivity will be open
                                    startActivity(new Intent(EditPostActivity.this, MainActivity.class));

                                }

                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }

                });
            }

        }


    }


}

