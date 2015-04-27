#!/usr/bin/perl 

#############################################################################
# Sends GolgiChat Verification E-Mail to Users
#############################################################################

use lib qw(. lib);

my $argsize = @ARGV;
if ($argsize != 2)
{
  exit();
}


# The first argument is the Group ID of the new User/Groupfrom
my $emailToSend = $ARGV[0];
my $verifCode = $ARGV[1];

print "$emailToSend\n";
print "$verifCode\n";

  
my $from = 'donotreply@openmindnetworks.com';
my $subject = 'Verification Code For GolgiChat';

my $message1 =<<"EOF";
<p>Dear GolgiChat User,</p>
EOF

         open(MAIL, "|/usr/sbin/sendmail -t");
 
         # Email Header
         print MAIL "Content-type: text/html\n";
         print MAIL "To: $emailToSend\n";
         print MAIL "From: $from\n";
         print MAIL "Subject: $subject\n\n";

         # Email Body
         #print MAIL $message1;

         print MAIL '<h2>Your GolgiChat verificiation Code is below</h2>';

         print MAIL "<p></p>";
         print MAIL "<p><b>Verification Code is $verifCode</b></p>";
         print MAIL "<p></p>";

         close(MAIL);
         print "Email Sent Successfully\n";
1;
