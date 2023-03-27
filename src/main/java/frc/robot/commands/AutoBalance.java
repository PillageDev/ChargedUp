package frc.robot.commands;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.Drivetrain;

public class AutoBalance extends CommandBase {
    private static final double PITCH_OFFSET = -90.0d; // Should be -70 on new bot
    private static final int BALANCE_THRESHOLD = 4;
    private static final int BALANCE_ITERATIONS = 20;
    private static final double INTERGRAL_LIMIT = 5;

    private double m_pitch;
    private double m_error = 0.0d;
    private double m_lastError = 0.0d;
    private double m_change = 0.0d;
    private double m_errorIntegral = 0.0d;
    private double m_velOutput = 0.0d;

    private double m_velMultipliedOutput = 0.0d;

    // start (gives throttle) (may make it overshoot if too high)
    private double m_P = 0.0015d;
    // finicky (depends on situation) (within 5 to 3 degress of error)
    private double m_I = 0.00d;
    // good rule of thumb for d: m_d = m_p * 10
    private double m_D = 0.04;

    // speed control
    private double m_clamp = 0.5;
    private int m_balancedTimes = 0;
    private double m_maxBalance = 20;
    private double vel = 2.5;

    private Drivetrain m_driveTrain;

    public AutoBalance(Drivetrain driveTrain) {

        m_driveTrain = driveTrain;
        addRequirements(driveTrain);

        SmartDashboard.putNumber("m_P", m_P);
        SmartDashboard.putNumber("m_I", m_I);
        SmartDashboard.putNumber("m_D", m_D);
        SmartDashboard.putNumber("m_Clamp", m_clamp);

        SmartDashboard.putNumber("vel", vel);
        SmartDashboard.putNumber("maxbalance", m_maxBalance);
    }

    /**
     * Checks the nearest number to the current number
     * 
     * @param back
     * @param num
     * @param front
     * @return the nearest number
     */

    public int CheckNearestNumber(int back, int num, int front) {
        int frontDiff = Math.abs(front - num);
        int backDiff = Math.abs(back - num);
        int retVal = backDiff;
        if (frontDiff < backDiff) {
            retVal = frontDiff;
        }
        return retVal;
    }

    @Override
    public void execute() {
        // calculate pid variables (error (p), change (d), error integral (i))
        m_P = SmartDashboard.getNumber("m_P", m_P);
        m_I = SmartDashboard.getNumber("m_I", m_I);
        m_D = SmartDashboard.getNumber("m_D", m_D);
        m_maxBalance = SmartDashboard.getNumber("maxbalance", m_maxBalance);
        m_clamp = SmartDashboard.getNumber("m_Clamp", m_clamp);
        vel = SmartDashboard.getNumber("vel", vel);

        m_lastError = m_error;

        m_pitch = m_driveTrain.getGyroPitchDegrees();
        m_error = m_pitch + PITCH_OFFSET;

        SmartDashboard.putNumber("Error", m_error);

        if (Math.abs(m_error) < BALANCE_THRESHOLD) {
            m_balancedTimes += 1;
        } else {
            m_balancedTimes = 0;
        }

        m_change = m_error - m_lastError;

        if (Math.abs(m_errorIntegral) < INTERGRAL_LIMIT) {
            // Accumulate the error into the integral
            m_errorIntegral += m_error;
        }

        SmartDashboard.putNumber("Error", m_error);
        SmartDashboard.putNumber("Change", m_change);
        SmartDashboard.putNumber("ErrorIntegral", m_errorIntegral);
        // calculate turning output using the pid
        m_velOutput = (m_P * m_error) + (m_I * m_errorIntegral) + (m_D * m_change);
        m_velOutput = MathUtil.clamp(m_velOutput, -m_clamp, m_clamp);
        SmartDashboard.putNumber("velOutput", m_velOutput);

        m_driveTrain.arcadeDrive(m_velMultipliedOutput, 0);

        SmartDashboard.putNumber("velMultipliedOutput", m_velMultipliedOutput);
        m_velMultipliedOutput = m_velOutput * vel;

        SmartDashboard.putNumber("BalancedTime", m_balancedTimes);
    }

    @Override
    public void end(boolean interrupted) {
        m_driveTrain.arcadeDrive(0, 0);
    }

    @Override
    public boolean isFinished() {
        return m_balancedTimes > BALANCE_ITERATIONS;
    }
}