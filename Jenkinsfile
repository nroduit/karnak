@Library('dsi-pipeline@v2') _
import ch.hcuge.dsi.jenkins.BuildImage;

pipelineSpringBootMaven(
    buildImage: BuildImage.MVN_39_JDK25,
    deployableType: "DOCKER",
    maven: [
        'checkstyleFailOnError': false,
        'archunitFailOnError': false,
        'pomParentFailOnError': false
    ],

    bitbucket: [
        'project': "TELIMA",
        'repository': "karnak"
    ],
    jira: [
        'projectKey': "TELIMA"
    ],
    sonar: [
        'disableSonar': false,
        'disableQG': false,
        'disableQGSecurity': false,
        'removeSonarSquid': false,
        'QGSecurityFailOnError': false,
        'QGFailOnError': false
    ]
)