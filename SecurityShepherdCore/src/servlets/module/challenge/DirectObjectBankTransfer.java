package servlets.module.challenge;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import utils.ShepherdLogManager;
import utils.Validate;
import dbProcs.Database;

/**
 * Insecure Direct Object Reference Bank Challenge Transfer Funds Function
 * DOES NOT RETURN RESULT KEY
 * <br/><br/>
 * This file is part of the Security Shepherd Project.
 * 
 * The Security Shepherd project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.<br/>
 * 
 * The Security Shepherd project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.<br/>
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Security Shepherd project.  If not, see <http://www.gnu.org/licenses/>. 
 * @author Mark Denihan
 *
 */
public class DirectObjectBankTransfer extends HttpServlet 
{
	private static final long serialVersionUID = 1L;
	private static org.apache.log4j.Logger log = Logger.getLogger(DirectObjectBankTransfer.class);
	private static String levelName = "Insecure Direct Object Bank Challenge (Transfer)";
	private static String levelHash = "1f0935baec6ba69d79cfb2eba5fdfa6ac5d77fadee08585eb98b130ec524d00c";
	/**
	 * This Servlet is used to register a new bank account
	 */
	public void doPost (HttpServletRequest request, HttpServletResponse response) 
	throws ServletException, IOException
	{
		//Setting IpAddress To Log and taking header for original IP if forwarded from proxy
		ShepherdLogManager.setRequestIp(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"));
		HttpSession ses = request.getSession(true);
		if(Validate.validateSession(ses))
		{
			ShepherdLogManager.setRequestIp(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"), ses.getAttribute("userName").toString());
			log.debug(levelName + " servlet accessed by: " + ses.getAttribute("userName").toString());
			PrintWriter out = response.getWriter();
			out.print(getServletInfo());
			boolean performTransfer = false;
			String errorMessage = new String();
			String applicationRoot = getServletContext().getRealPath("");
			try
			{
				String senderAccountNumber = request.getParameter("senderAccountNumber");
				log.debug("Sender Account Number - " + senderAccountNumber);
				String recieverAccountNumber = request.getParameter("recieverAccountNumber");
				log.debug("Reciever Account Number - " + recieverAccountNumber);
				String transferAmountString = request.getParameter("transferAmount");
				log.debug("Transfer Amount - " + transferAmountString);
				float tranferAmount = Float.parseFloat(transferAmountString);
				
				//Data Validation
				//Positive Transfer Amount?
				if(tranferAmount > 0)
				{
					//Sender Account Has necessary funds?
					float senderFunds = DirectObjectBankLogin.getAccountBalance(senderAccountNumber, applicationRoot);
					if((senderFunds-tranferAmount) > 0)
					{
						performTransfer = true;
					}
					else
						errorMessage = "Your account does not have the necessary funds to transfer that amount.";
				}
				else
					errorMessage = "The amount being transfered must be greater than zero.";
				
				String htmlOutput = new String();
				if(performTransfer)
				{
					log.debug("Valid Data Submitted, transfering Funds...");
					Connection conn = Database.getChallengeConnection(applicationRoot, "directObjectBank");
					CallableStatement callstmt = conn.prepareCall("CALL transferFunds(?, ?, ?)");
					callstmt.setString(1, senderAccountNumber);
					callstmt.setString(2, recieverAccountNumber);
					callstmt.setFloat(3, tranferAmount);
					callstmt.execute();
					log.debug("Sucessfully ran Transfer Funds procedure.");
					htmlOutput = "Funds have been transfered sucessfully!";
					Database.closeConnection(conn);
				}
				else
				{
					log.debug("Invalid Data Detected: " + errorMessage);
					htmlOutput = "An Error Occured: " + errorMessage;
				}
				log.debug("Outputting HTML");
				out.write(htmlOutput);
			}
			catch(SQLException e)
			{
				out.write("An Error Occurred! You must be getting funky! Could not get Transfer Funds!");
				log.fatal(levelName + " SQL Error - " + e.toString());
			}
			catch(Exception e)
			{
				out.write("An Error Occurred! You must be getting funky!");
				log.fatal(levelName + " - " + e.toString());
			}
		}
		else
		{
			log.error(levelName + " servlet accessed with no session");
		}
	}
}
