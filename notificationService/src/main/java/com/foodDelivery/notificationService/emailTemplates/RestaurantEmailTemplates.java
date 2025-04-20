package com.foodDelivery.notificationService.emailTemplates;

import com.foodDelivery.restaurantService.event.RestaurantEvent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RestaurantEmailTemplates {

    public String createRestaurantCreationEmail(RestaurantEvent event, List<String> cuisineNames) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; background-color: #f9f9f9; }
                .container { max-width: 600px; margin: 0 auto; background-color: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 3px 10px rgba(0,0,0,0.1); }
                .header { background: linear-gradient(135deg, #FF5722, #FF4500); color: white; padding: 25px; text-align: center; }
                .header h1 { margin: 0; font-size: 24px; text-shadow: 1px 1px 2px rgba(0,0,0,0.2); }
                .logo { margin-bottom: 15px; }
                .content { padding: 30px; }
                .card { background-color: #fff; border-left: 4px solid #FF4500; padding: 15px; margin: 15px 0; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
                .info-label { font-weight: bold; color: #555; display: block; margin-bottom: 5px; }
                .info-value { background-color: #f8f9fa; padding: 10px; border-radius: 4px; margin-bottom: 15px; }
                .cuisine-tag { display: inline-block; background-color: #FFF0E9; color: #FF4500; padding: 5px 10px; margin: 3px; border-radius: 20px; font-size: 14px; }
                .button { display: inline-block; background-color: #FF4500; color: white; text-decoration: none; padding: 12px 25px; border-radius: 4px; margin-top: 15px; font-weight: bold; }
                .footer { background-color: #f4f4f4; padding: 20px; text-align: center; font-size: 14px; color: #777; }
                .social-links { margin-top: 15px; }
                .social-link { display: inline-block; margin: 0 10px; color: #555; text-decoration: none; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo">🍽️</div>
                    <h1>Restaurant Successfully Created!</h1>
                </div>
                <div class="content">
                    <h2>Congratulations!</h2>
                    <p>Your restaurant <strong>"%s"</strong> has been successfully created on FlavorFleet.</p>
                    
                    <div class="card">
                        <span class="info-label">Restaurant ID:</span>
                        <div class="info-value">%s</div>
                        
                        <span class="info-label">Cuisine Types:</span>
                        <div class="info-value">
                            %s
                        </div>
                    </div>
                    
                    <p>You can now log in to manage your restaurant, add menu items, and start accepting orders.</p>
                    <p>Our onboarding team will contact you shortly to assist with any questions you may have.</p>
                    
                    <a href="https://flavorfleet.com/login" class="button">Log In to Dashboard</a>
                </div>
                <div class="footer">
                    <p>&copy; %d FlavorFleet. All rights reserved.</p>
                    <div class="social-links">
                        <a href="#" class="social-link">Help Center</a> | 
                        <a href="#" class="social-link">Contact Us</a> | 
                        <a href="#" class="social-link">Privacy Policy</a>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.formatted(
                event.getRestaurantName(),
                event.getRestaurantId(),
                formatCuisineNames(cuisineNames),
                java.time.Year.now().getValue()
        );
    }

    public String createAdminAddedEmail(String adminName, String restaurantName, List<String> cuisineNames) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; background-color: #f9f9f9; }
                .container { max-width: 600px; margin: 0 auto; background-color: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 3px 10px rgba(0,0,0,0.1); }
                .header { background: linear-gradient(135deg, #4CAF50, #2E7D32); color: white; padding: 25px; text-align: center; }
                .header h1 { margin: 0; font-size: 24px; text-shadow: 1px 1px 2px rgba(0,0,0,0.2); }
                .logo { margin-bottom: 15px; }
                .content { padding: 30px; }
                .card { background-color: #fff; border-left: 4px solid #4CAF50; padding: 15px; margin: 15px 0; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
                .info-label { font-weight: bold; color: #555; display: block; margin-bottom: 5px; }
                .info-value { background-color: #f8f9fa; padding: 10px; border-radius: 4px; margin-bottom: 15px; }
                .cuisine-tag { display: inline-block; background-color: #E8F5E9; color: #2E7D32; padding: 5px 10px; margin: 3px; border-radius: 20px; font-size: 14px; }
                .admin-capabilities { margin-top: 20px; }
                .capability-item { margin-bottom: 8px; padding-left: 25px; position: relative; }
                .capability-item:before { content: "✓"; color: #4CAF50; position: absolute; left: 0; font-weight: bold; }
                .button { display: inline-block; background-color: #4CAF50; color: white; text-decoration: none; padding: 12px 25px; border-radius: 4px; margin-top: 15px; font-weight: bold; }
                .footer { background-color: #f4f4f4; padding: 20px; text-align: center; font-size: 14px; color: #777; }
                .social-links { margin-top: 15px; }
                .social-link { display: inline-block; margin: 0 10px; color: #555; text-decoration: none; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo">👨‍🍳</div>
                    <h1>You're Now a Restaurant Admin</h1>
                </div>
                <div class="content">
                    <h2>Hello %s,</h2>
                    <p>You have been added as an administrator for the restaurant <strong>'%s'</strong>.</p>
                    
                    <div class="card">
                        <span class="info-label">Restaurant Cuisine Types:</span>
                        <div class="info-value">
                            %s
                        </div>
                        
                        <div class="admin-capabilities">
                            <p><strong>As an administrator, you can now:</strong></p>
                            <div class="capability-item">Manage restaurant details and operating hours</div>
                            <div class="capability-item">Create and update menu items</div>
                            <div class="capability-item">Monitor and process incoming orders</div>
                            <div class="capability-item">Update restaurant availability status</div>
                            <div class="capability-item">View reports and analytics</div>
                        </div>
                    </div>
                    
                    <p>Log in to your account to start managing the restaurant.</p>
                    <a href="https://flavorfleet.com/login" class="button">Access Dashboard</a>
                </div>
                <div class="footer">
                    <p>&copy; %d FlavorFleet. All rights reserved.</p>
                    <div class="social-links">
                        <a href="#" class="social-link">Help Center</a> | 
                        <a href="#" class="social-link">Contact Us</a> | 
                        <a href="#" class="social-link">Privacy Policy</a>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.formatted(
                adminName,
                restaurantName,
                formatCuisineNames(cuisineNames),
                java.time.Year.now().getValue()
        );
    }

    public String createAdminRemovedEmail(String adminName, String restaurantName) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; background-color: #f9f9f9; }
                .container { max-width: 600px; margin: 0 auto; background-color: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 3px 10px rgba(0,0,0,0.1); }
                .header { background: linear-gradient(135deg, #607D8B, #455A64); color: white; padding: 25px; text-align: center; }
                .header h1 { margin: 0; font-size: 24px; text-shadow: 1px 1px 2px rgba(0,0,0,0.2); }
                .logo { margin-bottom: 15px; }
                .content { padding: 30px; }
                .card { background-color: #fff; border-left: 4px solid #607D8B; padding: 15px; margin: 15px 0; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
                .message { background-color: #ECEFF1; padding: 15px; border-radius: 4px; margin: 20px 0; }
                .button { display: inline-block; background-color: #607D8B; color: white; text-decoration: none; padding: 12px 25px; border-radius: 4px; margin-top: 15px; font-weight: bold; }
                .footer { background-color: #f4f4f4; padding: 20px; text-align: center; font-size: 14px; color: #777; }
                .social-links { margin-top: 15px; }
                .social-link { display: inline-block; margin: 0 10px; color: #555; text-decoration: none; }
                .contact-options { margin-top: 20px; }
                .contact-option { margin-bottom: 10px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo">🔔</div>
                    <h1>Administrator Access Removed</h1>
                </div>
                <div class="content">
                    <h2>Hello %s,</h2>
                    <p>This is to inform you that your administrator access for the restaurant <strong>'%s'</strong> has been removed.</p>
                    
                    <div class="card">
                        <div class="message">
                            <p>You will no longer be able to manage this restaurant's details, menu items, or orders.</p>
                            <p>If you believe this was done in error, please contact the restaurant owner or our support team.</p>
                        </div>
                        
                        <div class="contact-options">
                            <p><strong>Need assistance?</strong></p>
                            <div class="contact-option">Email: <a href="mailto:support@flavorfleet.com">support@flavorfleet.com</a></div>
                            <div class="contact-option">Phone: (555) 123-4567</div>
                            <div class="contact-option">Help Center: <a href="https://flavorfleet.com/help">flavorfleet.com/help</a></div>
                        </div>
                    </div>
                    
                    <a href="https://flavorfleet.com/contact" class="button">Contact Support</a>
                </div>
                <div class="footer">
                    <p>&copy; %d FlavorFleet. All rights reserved.</p>
                    <div class="social-links">
                        <a href="#" class="social-link">Help Center</a> | 
                        <a href="#" class="social-link">Contact Us</a> | 
                        <a href="#" class="social-link">Privacy Policy</a>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.formatted(
                adminName,
                restaurantName,
                java.time.Year.now().getValue()
        );
    }

    public String createRestaurantUpdateEmail(RestaurantEvent event, List<String> cuisineNames) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; background-color: #f9f9f9; }
                .container { max-width: 600px; margin: 0 auto; background-color: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 3px 10px rgba(0,0,0,0.1); }
                .header { background: linear-gradient(135deg, #3498db, #2980b9); color: white; padding: 25px; text-align: center; }
                .header h1 { margin: 0; font-size: 24px; text-shadow: 1px 1px 2px rgba(0,0,0,0.2); }
                .logo { margin-bottom: 15px; font-size: 36px; }
                .content { padding: 30px; }
                .card { background-color: #fff; border-left: 4px solid #3498db; padding: 15px; margin: 15px 0; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
                .info-label { font-weight: bold; color: #555; display: block; margin-bottom: 5px; }
                .info-value { background-color: #f8f9fa; padding: 10px; border-radius: 4px; margin-bottom: 15px; }
                .cuisine-tag { display: inline-block; background-color: #E3F2FD; color: #2980b9; padding: 5px 10px; margin: 3px; border-radius: 20px; font-size: 14px; }
                .button { display: inline-block; background-color: #3498db; color: white; text-decoration: none; padding: 12px 25px; border-radius: 4px; margin-top: 15px; font-weight: bold; }
                .footer { background-color: #f4f4f4; padding: 20px; text-align: center; font-size: 14px; color: #777; }
                .social-links { margin-top: 15px; }
                .social-link { display: inline-block; margin: 0 10px; color: #555; text-decoration: none; }
                .highlight { font-weight: bold; color: #2980b9; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo">🔄</div>
                    <h1>Restaurant Successfully Updated</h1>
                </div>
                <div class="content">
                    <h2>Hello,</h2>
                    <p>Your restaurant <span class="highlight">"%s"</span> has been successfully updated on FlavorFleet.</p>
                    <div class="card">
                        <span class="info-label">Current Cuisine Types:</span>
                        <div class="info-value">
                            %s
                        </div>
                        <p>These changes have been applied to your restaurant profile and are now visible to customers.</p>
                    </div>
                    <p>Log in to review the changes and continue managing your restaurant.</p>
                    <a href="https://flavorfleet.com/login" class="button">Review Changes</a>
                </div>
                <div class="footer">
                    <p>&copy; %d FlavorFleet. All rights reserved.</p>
                    <div class="social-links">
                        <a href="#" class="social-link">Help Center</a> |
                        <a href="#" class="social-link">Contact Us</a> |
                        <a href="#" class="social-link">Privacy Policy</a>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.formatted(
                event.getRestaurantName(),
                formatCuisineNames(cuisineNames),
                java.time.Year.now().getValue()
        );
    }

    private String formatCuisineNames(List<String> cuisineNames) {
        if (cuisineNames.isEmpty() || (cuisineNames.size() == 1 && cuisineNames.get(0).equals("Not specified"))) {
            return "<span class=\"cuisine-tag\">Not specified</span>";
        }

        return cuisineNames.stream()
                .map(name -> "<span class=\"cuisine-tag\">" + name + "</span>")
                .collect(Collectors.joining(" "));
    }
}
