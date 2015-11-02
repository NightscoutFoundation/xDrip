package com.eveningoutpost.dexdrip.ShareModels;

import com.eveningoutpost.dexdrip.ShareModels.Models.ExistingFollower;
import com.eveningoutpost.dexdrip.ShareModels.Models.InvitationPayload;
import com.eveningoutpost.dexdrip.ShareModels.Models.ShareAuthenticationBody;
import com.eveningoutpost.dexdrip.ShareModels.Models.ShareUploadPayload;
import com.eveningoutpost.dexdrip.ShareModels.UserAgentInfo.UserAgent;
import com.squareup.okhttp.ResponseBody;

import java.util.List;
import java.util.Map;

import retrofit.Call;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.http.QueryMap;

/**
 * Created by stephenblack on 3/16/15.
 */
public interface DexcomShareInterface {
    @POST("General/LoginPublisherAccountByName")
    Call<ResponseBody> getSessionId(@Body ShareAuthenticationBody body);
    //Since this seems to respond with a string we need a callback that will parse the response body
    //new String(((TypedByteArray) response.getBody()).getBytes());

    @POST("Publisher/IsRemoteMonitoringSessionActive")
    Call<ResponseBody> checkSessionActive(@QueryMap Map<String, String> options);
    // needs ?sessionId={YourSessionId}
    // returns true or false

    @POST("Publisher/StartRemoteMonitoringSession")
    Call<ResponseBody> StartRemoteMonitoringSession(@QueryMap Map<String, String> options);
    // needs ?sessionId={YourSessionId}&serialNumber={YourdexcomSerialNumber}
    // returns status code

    @POST("Publisher/PostReceiverEgvRecords")
    Call<ResponseBody> uploadBGRecords(@QueryMap Map<String, String> options, @Body ShareUploadPayload payload);
    // needs ?sessionId={YourSessionId}
    // body ShareUploadPayload
    // returns status code

    @POST("General/AuthenticatePublisherAccount")
    Call<ResponseBody> authenticatePublisherAccount(@Body ShareAuthenticationBody body, @QueryMap Map<String, String> options);
    // maybe needs ?sessionId={YourSessionId}&serialNumber={YourdexcomSerialNumber}
    // body ShareUploadPayload
    // returns status code

    @POST("Publisher/CheckMonitoredReceiverAssignmentStatus")
    Call<ResponseBody> checkMonitorAssignment(@QueryMap Map<String, String> options);
    // needs ?sessionId={YourSessionId}&serialNumber={YourdexcomSerialNumber}
    // returns `AssignedToYou` or `NotAssigned`

    @POST("/Publisher/ReplacePublisherAccountMonitoredReceiver")
    Call<ResponseBody> updateMonitorAssignment(@QueryMap Map<String, String> options);
    // needs ?sessionId={YourSessionId}&serialNumber={YourdexcomSerialNumber}
    // returns status code?


    @POST("/Publisher/UpdatePublisherAccountRuntimeInfo")
    Call<ResponseBody> updatePublisherAccountInfo(@Body UserAgent body);
    //Since this seems to respond with a string we need a callback that will parse the response body
    //new String(((TypedByteArray) response.getBody()).getBytes());



    //Follower Related
    @POST("/Publisher/ListPublisherAccountSubscriptions")
   Call<ResponseBody> getContacts(@QueryMap Map<String, String> options, Callback<List<ExistingFollower>> callback);
    // needs ?sessionId={YourSessionId}
    // returns
    // [
    //    {
    //        "ContactId":"FollowersContactId",
    //            "ContactName":"FollowersName",
    //            "DateTimeCreated":{
    //        "DateTime":"\/Date(1437101121008)\/",
    //                "OffsetMinutes":0
    //          },
    //        "DateTimeModified":{
    //        "DateTime":"\/Date(1437101121008)\/",
    //                "OffsetMinutes":0
    //          },
    //        "DisplayName":"YourDisplayName",
    //            "InviteExpires":{
    //        "DateTime":"\/Date(1437705921008)\/",
    //                "OffsetMinutes":0
    //          },
    //        "IsEnabled":false,
    //        "IsMonitoringSessionActive":true,
    //        "Permissions":1,
    //        "State":2,
    //        "SubscriberId":"00000000-0000-0000-0000-000000000000",
    //        "SubscriptionId":"theirSubscriptionIdIsuppose?"
    //    }
    //]

    @POST("Publisher/DoesContactExistByName")
    Call<ResponseBody> doesContactExist(@QueryMap Map<String, String> options);
    // needs ?sessionId={YourSessionId}&contactName={newcontactName}
    // returns true or false

    @POST("Publisher/CreateContact")
    Call<ResponseBody> createContact(@QueryMap Map<String, String> options);
    // needs ?sessionId={YourSessionId}&contactName={newcontactName}&emailAddress={FollowerEmail}
    // returns a contact id

    @POST("Publisher/CreateSubscriptionInvitation")
    Call<ResponseBody> createInvitationForContact(@Body InvitationPayload body, @QueryMap Map<String, String> options);
    // needs ?sessionId={YourSessionId}&contactId={ContactId}
    // returns a contact id

    @POST("Publisher/DeleteContact")
    Call<ResponseBody> deleteContact(@QueryMap Map<String, String> options);
    // needs ?sessionId={YourSessionId}&contactId={foll`owersContactId}
    // just a status

}
