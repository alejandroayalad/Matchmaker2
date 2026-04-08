package com.alejandro.botjobhunter.service.email;

import com.alejandro.botjobhunter.dto.EmailJobResultDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedInEmailParserTest {

    private final LinkedInEmailParser parser = new LinkedInEmailParser();

    @Test
    void parseShouldExtractJobsFromLinkedInDigestMarkup() {
        String html = """
                <div dir="ltr" style="margin:0px;width:100%;background-color:#f3f2f0">
                  <table role="presentation" width="512" align="center">
                    <tbody>
                      <tr>
                        <td style="padding-left:24px;padding-right:24px;padding-bottom:24px">
                          <div>
                            <table role="presentation" width="100%" style="line-height:1.25">
                              <tbody>
                                <tr>
                                  <td style="padding-top:24px">
                                    <table role="presentation" width="100%">
                                      <tbody>
                                        <tr>
                                          <td>
                                            <table role="presentation" width="100%">
                                              <tbody>
                                                <tr>
                                                  <td valign="top" style="width:48px;padding-right:8px" width="48">
                                                    <a href="https://www.linkedin.com/comm/jobs/view/4397726012/?trackingId=logo-link"
                                                       style="color:#0a66c2;display:inline-block;text-decoration:none"
                                                       target="_blank">
                                                      <img alt="Quik Hire Staffing">
                                                    </a>
                                                  </td>
                                                  <td valign="top">
                                                    <a href="https://www.linkedin.com/comm/jobs/view/4397726012/?trackingId=outer-link"
                                                       style="color:#0a66c2;display:inline-block;text-decoration:none"
                                                       target="_blank">
                                                      <table role="presentation" width="100%">
                                                        <tbody>
                                                          <tr>
                                                            <td style="padding-bottom:0px">
                                                              <a href="https://www.linkedin.com/comm/jobs/view/4397726012/?trackingId=jobcard_body_0"
                                                                 style="color:#0a66c2;display:inline-block;text-decoration:none;font-size:16px;font-weight:600;line-height:1.25"
                                                                 target="_blank">
                                                                Junior Software Developer - Remote
                                                              </a>
                                                            </td>
                                                          </tr>
                                                          <tr>
                                                            <td style="padding-bottom:0px">
                                                              <p style="margin:0;font-weight:400;margin-top:4px;text-overflow:ellipsis;font-size:12px;line-height:1.25;color:#1f1f1f;overflow:hidden;display:-webkit-box">
                                                                Quik Hire Staffing · Mexico (En remoto)
                                                              </p>
                                                            </td>
                                                          </tr>
                                                        </tbody>
                                                      </table>
                                                    </a>
                                                  </td>
                                                </tr>
                                              </tbody>
                                            </table>
                                          </td>
                                        </tr>
                                      </tbody>
                                    </table>
                                  </td>
                                </tr>
                                <tr>
                                  <td style="padding-top:24px">
                                    <table role="presentation" width="100%">
                                      <tbody>
                                        <tr>
                                          <td>
                                            <table role="presentation" width="100%">
                                              <tbody>
                                                <tr>
                                                  <td valign="top" style="width:48px;padding-right:8px" width="48">
                                                    <a href="https://www.linkedin.com/comm/jobs/view/4397704898/?trackingId=logo-link-2"
                                                       style="color:#0a66c2;display:inline-block;text-decoration:none"
                                                       target="_blank">
                                                      <img alt="Quik Hire Staffing">
                                                    </a>
                                                  </td>
                                                  <td valign="top">
                                                    <a href="https://www.linkedin.com/comm/jobs/view/4397704898/?trackingId=outer-link-2"
                                                       style="color:#0a66c2;display:inline-block;text-decoration:none"
                                                       target="_blank">
                                                      <table role="presentation" width="100%">
                                                        <tbody>
                                                          <tr>
                                                            <td style="padding-bottom:0px">
                                                              <a href="https://www.linkedin.com/comm/jobs/view/4397704898/?trackingId=jobcard_body_1"
                                                                 style="color:#0a66c2;display:inline-block;text-decoration:none;font-size:16px;font-weight:600;line-height:1.25"
                                                                 target="_blank">
                                                                Junior Software Engineer - Remote
                                                              </a>
                                                            </td>
                                                          </tr>
                                                          <tr>
                                                            <td style="padding-bottom:0px">
                                                              <p style="margin:0;font-weight:400;margin-top:4px;text-overflow:ellipsis;font-size:12px;line-height:1.25;color:#1f1f1f;overflow:hidden;display:-webkit-box">
                                                                Quik Hire Staffing · Mexico (En remoto)
                                                              </p>
                                                            </td>
                                                          </tr>
                                                        </tbody>
                                                      </table>
                                                    </a>
                                                  </td>
                                                </tr>
                                              </tbody>
                                            </table>
                                          </td>
                                        </tr>
                                      </tbody>
                                    </table>
                                  </td>
                                </tr>
                                <tr>
                                  <td style="padding-top:24px">
                                    <table role="presentation" width="100%">
                                      <tbody>
                                        <tr>
                                          <td>
                                            <table role="presentation" width="100%">
                                              <tbody>
                                                <tr>
                                                  <td valign="top" style="width:48px;padding-right:8px" width="48">
                                                    <a href="https://www.linkedin.com/comm/jobs/view/4397704898/?trackingId=logo-link-duplicate"
                                                       style="color:#0a66c2;display:inline-block;text-decoration:none"
                                                       target="_blank">
                                                      <img alt="Quik Hire Staffing">
                                                    </a>
                                                  </td>
                                                  <td valign="top">
                                                    <a href="https://www.linkedin.com/comm/jobs/view/4397704898/?trackingId=outer-link-duplicate"
                                                       style="color:#0a66c2;display:inline-block;text-decoration:none"
                                                       target="_blank">
                                                      <table role="presentation" width="100%">
                                                        <tbody>
                                                          <tr>
                                                            <td style="padding-bottom:0px">
                                                              <a href="https://www.linkedin.com/comm/jobs/view/4397704898/?trackingId=jobcard_body_duplicate"
                                                                 style="color:#0a66c2;display:inline-block;text-decoration:none;font-size:16px;font-weight:600;line-height:1.25"
                                                                 target="_blank">
                                                                Junior Software Engineer - Remote
                                                              </a>
                                                            </td>
                                                          </tr>
                                                          <tr>
                                                            <td style="padding-bottom:0px">
                                                              <p style="margin:0;font-weight:400;margin-top:4px;text-overflow:ellipsis;font-size:12px;line-height:1.25;color:#1f1f1f;overflow:hidden;display:-webkit-box">
                                                                Quik Hire Staffing · Mexico (En remoto)
                                                              </p>
                                                            </td>
                                                          </tr>
                                                        </tbody>
                                                      </table>
                                                    </a>
                                                  </td>
                                                </tr>
                                              </tbody>
                                            </table>
                                          </td>
                                        </tr>
                                      </tbody>
                                    </table>
                                  </td>
                                </tr>
                              </tbody>
                            </table>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                """;

        List<EmailJobResultDTO> results = parser.parse(html);

        assertEquals(2, results.size());
        assertEquals(
                new EmailJobResultDTO(
                        "Junior Software Developer - Remote",
                        "Quik Hire Staffing",
                        "Mexico (En remoto)",
                        "https://www.linkedin.com/jobs/view/4397726012"
                ),
                results.get(0)
        );
        assertEquals(
                new EmailJobResultDTO(
                        "Junior Software Engineer - Remote",
                        "Quik Hire Staffing",
                        "Mexico (En remoto)",
                        "https://www.linkedin.com/jobs/view/4397704898"
                ),
                results.get(1)
        );
    }

    @Test
    void isLikelyJobAlertEmailShouldAcceptLinkedInAlerts() {
        String html = """
                <html>
                  <body>
                    <a href="https://www.linkedin.com/comm/jobs/view/4397726012/?trackingId=jobcard_body_0"
                       style="font-weight:600">Junior Software Developer - Remote</a>
                    <p>LinkedIn</p>
                  </body>
                </html>
                """;

        assertTrue(parser.isLikelyJobAlertEmail(
                "LinkedIn Job Alert",
                "jobs-noreply@linkedin.com",
                html
        ));
    }

    @Test
    void isLikelyJobAlertEmailShouldRejectNonLinkedInEmails() {
        String html = """
                <html>
                  <body>
                    <a href="https://example.com/jobs/view/4397726012"
                       style="font-weight:600">Junior Software Developer - Remote</a>
                  </body>
                </html>
                """;

        assertFalse(parser.isLikelyJobAlertEmail(
                "Weekly digest",
                "newsletter@example.com",
                html
        ));
    }

    @Test
    void parseShouldNormalizeTrackingUrlsAndIgnoreMalformedLinks() {
        String html = """
                <html>
                  <body>
                    <table>
                      <tr>
                        <td>
                          <a href="https://www.linkedin.com/track?url=https%3A%2F%2Fwww.linkedin.com%2Fjobs%2Fview%2F5555%2F%3FtrackingId%3Dabc"
                             style="font-weight:600">
                            Mid Backend Engineer
                          </a>
                          <p style="color:#1f1f1f;font-size:12px">Acme Corp · Remote</p>
                        </td>
                      </tr>
                      <tr>
                        <td>
                          <a href="https://www.linkedin.com/jobs/view/not-a-real-id"
                             style="font-weight:600">
                            Broken Job
                          </a>
                          <p style="color:#1f1f1f;font-size:12px">Broken Inc · Remote</p>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """;

        List<EmailJobResultDTO> results = parser.parse(html);

        assertEquals(1, results.size());
        assertEquals(
                new EmailJobResultDTO(
                        "Mid Backend Engineer",
                        "Acme Corp",
                        "Remote",
                        "https://www.linkedin.com/jobs/view/5555"
                ),
                results.get(0)
        );
    }
}
