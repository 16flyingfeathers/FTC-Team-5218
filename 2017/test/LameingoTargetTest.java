package test;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;

import team25core.Robot;
import team25core.RobotEvent;
import team25core.RobotNavigation;
import team25core.Robot_TwoWheelDrive;
import team25core.TwoWheelDirectDrivetrain;

import static test.LameingoTargetTest.TargetState.ALIGNED;
import static test.LameingoTargetTest.TargetState.AT_TARGET;
import static test.LameingoTargetTest.TargetState.SQUARE_UP_PHASE_1;
import static test.LameingoTargetTest.TargetState.SQUARE_UP_PHASE_2;

/*
 * FTC Team 25: cmacfarl, January 31, 2017
 */

@Autonomous(name = "TEST Target", group = "AutoTest")
public class LameingoTargetTest extends Robot {

    enum TargetState {
        FIND_TARGET,
        LOST_TARGET,
        INITIAL_APPROACH,
        SQUARE_UP_PHASE_1,
        SQUARE_UP_PHASE_2,
        SQUARE_UP_PHASE_3,
        FINAL_APPROACH,
        AT_TARGET,
        ALIGNED,
    };

    RobotNavigation nav;
    TwoWheelDirectDrivetrain drive;
    DcMotor leftMotor;
    DcMotor rightMotor;
    TargetState state;
    double bearing;

    // Select which camera you want use.  The FRONT camera is the one on the same side as the screen.  Alt. is BACK
    private static final VuforiaLocalizer.CameraDirection CAMERA_CHOICE = VuforiaLocalizer.CameraDirection.FRONT;
    
    @Override
    public void handleEvent(RobotEvent e) {

    }

    @Override
    public void init()
    {
        leftMotor = hardwareMap.dcMotor.get("leftMotor");
        rightMotor = hardwareMap.dcMotor.get("rightMotor");
        leftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        drive = new TwoWheelDirectDrivetrain(0, rightMotor, leftMotor);
        nav = new RobotNavigation(this, drive);

        // VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters(R.id.cameraMonitorViewId);  // Use this line to see camera display
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();                             // OR... Use this line to improve performance

        // Get your own Vuforia key at  https://developer.vuforia.com/license-manager
        // and paste it here...
        parameters.vuforiaLicenseKey = "Afbu2Uv/////AAAAGVouNdSAD0P8la+sq37vCdQ6uLVH8NWrBLnfZ1R5rObJQpVVHJzqvIgMZO5gTqXG6DYJZcgwtSVZXU2g20FAJobxCog9Wc5vtqgJJmrsJ0NOABRbi9vy4Y9IzBVfaDoRsQTmjxxFf62Z9slttsb44KopGpVGTQ83iHnTo/wDvnZBWRhmckG6IKuqkbRYCFD+w1hHvVLuDoIYLgfpa1Rw1Pc7rszP/CDzUfeO9KwodFpEsfZHIZI8KHIYzfRIOhg1Tg0T4eRsLCO8s9vfZd6vfTuUA/sZkID3N7BsrlLaL6vUqheGPvsbPuQQsMqgPNYTqbhvv3KI/SR5WxUaccuVHnpVMhAjkdpruWVliCCZqp1t";

        parameters.cameraDirection = CAMERA_CHOICE;
        parameters.useExtendedTracking = false;
        VuforiaLocalizer vuforia = ClassFactory.createVuforiaLocalizer(parameters);

        /**
         * Load the data sets that for the trackable objects we wish to track.
         * These particular data sets are stored in the 'assets' part of our application
         * They represent the four image targets used in the 2016-17 FTC game.
         */
        VuforiaTrackables targets = vuforia.loadTrackablesFromAsset("FTC_2016-17");
        targets.get(0).setName("Blue Near");
        targets.get(1).setName("Red Far");
        targets.get(2).setName("Blue Far");
        targets.get(3).setName("Red Near");

        /**
         * Create a transformation matrix describing where the phone is on the robot.
         *
         * The coordinate frame for the robot looks the same as the field.
         * The robot's "forward" direction is facing out along X axis, with the LEFT side facing out along the Y axis.
         * Z is UP on the robot.  This equates to a bearing angle of Zero degrees.
         *
         * The phone starts out lying flat, with the screen facing Up and with the physical top of the phone
         * pointing to the LEFT side of the Robot.  If we consider that the camera and screen will be
         * in "Landscape Mode" the upper portion of the screen is closest to the front of the robot.
         *
         * If using the rear (High Res) camera:
         * We need to rotate the camera around it's long axis to bring the rear camera forward.
         * This requires a negative 90 degree rotation on the Y axis
         *
         * If using the Front (Low Res) camera
         * We need to rotate the camera around it's long axis to bring the FRONT camera forward.
         * This requires a Positive 90 degree rotation on the Y axis
         *
         * Next, translate the camera lens to where it is on the robot.
         * In this example, it is centered (left to right), but 110 mm forward of the middle of the robot, and 200 mm above ground level.
         */

        final int CAMERA_FORWARD_DISPLACEMENT  = 146;   // Camera is 110 mm in front of robot center
        final int CAMERA_VERTICAL_DISPLACEMENT = 190;   // Camera is 200 mm above ground
        final int CAMERA_LEFT_DISPLACEMENT     = 1;     // Camera is ON the robots center line

        OpenGLMatrix phoneLocationOnRobot = OpenGLMatrix
                .translation(CAMERA_FORWARD_DISPLACEMENT, CAMERA_LEFT_DISPLACEMENT, CAMERA_VERTICAL_DISPLACEMENT)
                .multiplied(Orientation.getRotationMatrix(
                        AxesReference.EXTRINSIC, AxesOrder.YZX,
                        AngleUnit.DEGREES, CAMERA_CHOICE == VuforiaLocalizer.CameraDirection.FRONT ? 90 : -90, 0, 0));

        nav.initVuforia(targets, parameters, phoneLocationOnRobot);

        nav.activateTracking();
    }

    @Override
    public void start()
    {
        state = TargetState.FIND_TARGET;
    }

    private final static double YAW_GAIN =  0.002;   // Rate at which we respond to heading error
    private final static double LATERAL_GAIN =  0.00015;  // Rate at which we respond to off-axis error
    private final static double AXIAL_GAIN =  0.0006;  // Rate at which we respond to target distance errors

    private final static double FINAL_YAW_GAIN =  0.002;   // Rate at which we respond to heading error
    private final static double FINAL_LATERAL_GAIN =  0.0015;  // Rate at which we respond to off-axis error
    private final static double FINAL_AXIAL_GAIN =  0.0006;  // Rate at which we respond to target distance errors

    @Override
    public void loop()
    {
        boolean visible;

        super.loop();

        visible = nav.targetsAreVisible();

        switch (state) {
        case FIND_TARGET:
            if (visible) {
                drive.stop();
                nav.addNavTelemetry();
                state = TargetState.INITIAL_APPROACH;
                nav.setGainParams(YAW_GAIN, AXIAL_GAIN, LATERAL_GAIN);
            } else {
                this.telemetry.addData("Rotating...", "");
                drive.turnRight(0.06);
            }
            break;
        case LOST_TARGET:
            if (visible) {
                state = TargetState.INITIAL_APPROACH;
            } else if (nav.getRelativeBearing() > 0) {
                drive.turnLeft(0.10);
            } else {
                drive.turnRight(0.10);
            }
            break;
        case INITIAL_APPROACH:
             if (!visible) {
                state = TargetState.LOST_TARGET;
             } else if (nav.cruiseControl(800)) {
                state = SQUARE_UP_PHASE_1;
             }
        case SQUARE_UP_PHASE_1:
            if (Math.abs(nav.getRobotBearing()) < 88.5) {
                drive.turnLeft(-0.10);
            } else {
                drive.stop();
                state = SQUARE_UP_PHASE_2;
            }
            break;
        case SQUARE_UP_PHASE_2:
            if (Math.abs(nav.getStrafe()) > 0) {
            }
            break;
        case SQUARE_UP_PHASE_3:
            break;
        case FINAL_APPROACH:
            if (nav.targetsAreVisible()) {
                if (nav.cruiseControl(200)) {
                    state = AT_TARGET;
                    bearing = nav.getRelativeBearing();
                }
            } else {
                RobotLog.i("Lost target %f", nav.getRelativeBearing());
                state = TargetState.LOST_TARGET;
            }
            break;
        case AT_TARGET:
            RobotLog.i("Entering state AT_TARGET");
            if ((nav.getRobotBearing() < 1.5) && (nav.getRobotBearing() > -1.5)) {
                state = ALIGNED;
            } else if (nav.getRobotBearing() > 0) {
                drive.turnLeft(0.10);
            } else {
                drive.turnRight(0.10);
            }
            break;
        case ALIGNED:
            RobotLog.i("Entering state ALIGNED");
            nav.addNavTelemetry();
            drive.stop();
            break;
        }


    }
}
