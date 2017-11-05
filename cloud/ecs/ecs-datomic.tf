provider "aws" {
  region = "${var.region}"
  access_key = "${var.aws_access_key_id}"
  secret_key = "${var.aws_secret_key}"
}

resource "aws_ecr_repository" "edgarly-registry" {
  name = "edgarly"
}

resource "aws_ecr_repository_policy" "edgarly-registry-policy" {
  repository = "${aws_ecr_repository.edgarly-registry.name}"

  policy = <<EOF
  {
      "Version": "2008-10-17",
      "Statement": [
          {
              "Sid": "new policy",
              "Effect": "Allow",
              "Principal": "*",
              "Action": [
                  "ecr:GetDownloadUrlForLayer",
                  "ecr:BatchGetImage",
                  "ecr:BatchCheckLayerAvailability",
                  "ecr:PutImage",
                  "ecr:InitiateLayerUpload",
                  "ecr:UploadLayerPart",
                  "ecr:CompleteLayerUpload",
                  "ecr:DescribeRepositories",
                  "ecr:GetRepositoryPolicy",
                  "ecr:ListImages",
                  "ecr:DeleteRepository",
                  "ecr:BatchDeleteImage",
                  "ecr:SetRepositoryPolicy",
                  "ecr:DeleteRepositoryPolicy"
              ]
          }
      ]
  }
  EOF
}

resource "aws_security_group" "datomic" {
  name = "datomic"
  description = "datomic"

  ingress {
    from_port = 4334
    to_port = 4334
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 4335
    to_port = 4335
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 4336
    to_port = 4336
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_cloudformation_stack" "edgarly-datomic" {

  name = "edgarly-datomic-stack"

  template_body = <<STACK
{"Resources":
 {"LaunchGroup":
  {"Type":"AWS::AutoScaling::AutoScalingGroup",
   "Properties":
   {"MinSize":{"Ref":"GroupSize"},
    "Tags":
    [{"Key":"Name",
      "Value":{"Ref":"AWS::StackName"},
      "PropagateAtLaunch":"true"}],
    "MaxSize":{"Ref":"GroupSize"},
    "AvailabilityZones":{"Fn::GetAZs":""},
    "LaunchConfigurationName":{"Ref":"LaunchConfig"}}},
  "LaunchConfig":
  {"Type":"AWS::AutoScaling::LaunchConfiguration",
   "Properties":
   {"ImageId":
    {"Fn::FindInMap":
     ["AWSRegionArch2AMI", {"Ref":"AWS::Region"},
      {"Fn::FindInMap":
       ["AWSInstanceType2Arch", {"Ref":"InstanceType"}, "Arch"]}]},
    "UserData":
    {"Fn::Base64":
     {"Fn::Join":
      ["\n",
       ["exec > >(tee \/var\/log\/user-data.log|logger -t user-data -s 2>\/dev\/console) 2>&1",
        {"Fn::Join":["=", ["export XMX", {"Ref":"Xmx"}]]},
        {"Fn::Join":["=", ["export JAVA_OPTS", {"Ref":"JavaOpts"}]]},
        {"Fn::Join":
         ["=",
          ["export DATOMIC_DEPLOY_BUCKET",
           {"Ref":"DatomicDeployBucket"}]]},
        {"Fn::Join":
         ["=", ["export DATOMIC_VERSION", {"Ref":"DatomicVersion"}]]},
        "cd \/datomic", "cat <<EOF >aws.properties",
        "host=`curl http:\/\/169.254.169.254\/latest\/meta-data\/local-ipv4`",
        "alt-host=`curl http:\/\/169.254.169.254\/latest\/meta-data\/public-ipv4`",
        "aws-dynamodb-region=us-east-1\naws-transactor-role=datomic-aws-transactor-4\naws-peer-role=datomic-aws-peer-4\nprotocol=ddb\nmemory-index-max=256m\nport=4334\nmemory-index-threshold=32m\nobject-cache-max=128m\nlicense-key=Ojm4RK1xkz5VY7dn5fNPDDrU6\/whneQxorSs5ybW\/VAn27ZRUQ7xIUZB8DPcfWVjcOBSWdvxnJaKnvedq10KOTgtYU1uADfPhOY6Is8T8LUyr\/99YYk1Zse7MFjaiqZ8JI3jRBcI+CrIQHJCYho9l7+hIOfOXpyScdzQXaPeMyHf1PNCePqbomoORbxz+zxAkwcHwLQ63NYpqhpzBbZcPOnWgJGByR0t1HOpAFnc\/YlTxJSUCvlY4imBBZRSRE7T+CaNvj4DMbaxSJbfJQKJjhYSMTO41emjsq8JIgQi2plu1SyLZrPFliNpEBxNDVECg2CZENzp\/x08p61PtSuVmg==\naws-dynamodb-table=your-system-name",
        "EOF", "chmod 744 aws.properties",
        "AWS_ACCESS_KEY_ID=\"$${DATOMIC_READ_DEPLOY_ACCESS_KEY_ID}\" AWS_SECRET_ACCESS_KEY=\"$${DATOMIC_READ_DEPLOY_AWS_SECRET_KEY}\" aws s3 cp \"s3:\/\/$${DATOMIC_DEPLOY_BUCKET}\/$${DATOMIC_VERSION}\/startup.sh\" startup.sh",
        "chmod 500 startup.sh", ".\/startup.sh"]]}},
    "InstanceType":{"Ref":"InstanceType"},
    "InstanceMonitoring":{"Ref":"InstanceMonitoring"},
    "SecurityGroups":{"Ref":"SecurityGroups"},
    "IamInstanceProfile":{"Ref":"InstanceProfile"},
    "BlockDeviceMappings":
    [{"DeviceName":"\/dev\/sdb", "VirtualName":"ephemeral0"}]}}},
 "Mappings":
 {"AWSInstanceType2Arch":
  {"m3.large":{"Arch":"64h"},
   "c4.8xlarge":{"Arch":"64h"},
   "t2.2xlarge":{"Arch":"64h"},
   "c3.large":{"Arch":"64h"},
   "hs1.8xlarge":{"Arch":"64h"},
   "i2.xlarge":{"Arch":"64h"},
   "r4.4xlarge":{"Arch":"64h"},
   "m1.small":{"Arch":"64p"},
   "m4.large":{"Arch":"64h"},
   "m4.xlarge":{"Arch":"64h"},
   "c3.8xlarge":{"Arch":"64h"},
   "m1.xlarge":{"Arch":"64p"},
   "cr1.8xlarge":{"Arch":"64h"},
   "m4.10xlarge":{"Arch":"64h"},
   "i3.8xlarge":{"Arch":"64h"},
   "m3.2xlarge":{"Arch":"64h"},
   "r4.large":{"Arch":"64h"},
   "c4.xlarge":{"Arch":"64h"},
   "t2.medium":{"Arch":"64h"},
   "t2.xlarge":{"Arch":"64h"},
   "c4.large":{"Arch":"64h"},
   "c3.2xlarge":{"Arch":"64h"},
   "m4.2xlarge":{"Arch":"64h"},
   "i3.2xlarge":{"Arch":"64h"},
   "m2.2xlarge":{"Arch":"64p"},
   "c4.2xlarge":{"Arch":"64h"},
   "cc2.8xlarge":{"Arch":"64h"},
   "hi1.4xlarge":{"Arch":"64p"},
   "m4.4xlarge":{"Arch":"64h"},
   "i3.16xlarge":{"Arch":"64h"},
   "r3.4xlarge":{"Arch":"64h"},
   "m1.large":{"Arch":"64p"},
   "m2.4xlarge":{"Arch":"64p"},
   "c3.4xlarge":{"Arch":"64h"},
   "r3.large":{"Arch":"64h"},
   "c4.4xlarge":{"Arch":"64h"},
   "r3.xlarge":{"Arch":"64h"},
   "m2.xlarge":{"Arch":"64p"},
   "r4.16xlarge":{"Arch":"64h"},
   "t2.large":{"Arch":"64h"},
   "m3.xlarge":{"Arch":"64h"},
   "i2.4xlarge":{"Arch":"64h"},
   "r4.8xlarge":{"Arch":"64h"},
   "i3.large":{"Arch":"64h"},
   "r3.8xlarge":{"Arch":"64h"},
   "c1.medium":{"Arch":"64p"},
   "r4.2xlarge":{"Arch":"64h"},
   "i2.8xlarge":{"Arch":"64h"},
   "m3.medium":{"Arch":"64h"},
   "r3.2xlarge":{"Arch":"64h"},
   "m1.medium":{"Arch":"64p"},
   "i3.4xlarge":{"Arch":"64h"},
   "m4.16xlarge":{"Arch":"64h"},
   "i3.xlarge":{"Arch":"64h"},
   "r4.xlarge":{"Arch":"64h"},
   "c1.xlarge":{"Arch":"64p"},
   "t1.micro":{"Arch":"64p"},
   "c3.xlarge":{"Arch":"64h"},
   "i2.2xlarge":{"Arch":"64h"},
   "t2.small":{"Arch":"64h"}},
  "AWSRegionArch2AMI":
  {"ap-northeast-1":{"64p":"ami-eb494d8c", "64h":"ami-81f7cde6"},
   "ap-northeast-2":{"64p":"ami-6eb66a00", "64h":"ami-f594489b"},
   "ca-central-1":{"64p":"ami-204bf744", "64h":"ami-5e5be73a"},
   "us-east-2":{"64p":"ami-5b42643e", "64h":"ami-896c4aec"},
   "eu-west-2":{"64p":"ami-e52d3a81", "64h":"ami-55091e31"},
   "us-west-1":{"64p":"ami-97cbebf7", "64h":"ami-442a0a24"},
   "ap-southeast-1":{"64p":"ami-db1492b8", "64h":"ami-3e90165d"},
   "us-west-2":{"64p":"ami-daa5c6ba", "64h":"ami-cb5030ab"},
   "eu-central-1":{"64p":"ami-f3f02b9c", "64h":"ami-d564bcba"},
   "us-east-1":{"64p":"ami-7f5f1e69", "64h":"ami-da5110cc"},
   "eu-west-1":{"64p":"ami-66001700", "64h":"ami-77465211"},
   "ap-southeast-2":{"64p":"ami-32cbdf51", "64h":"ami-66647005"},
   "ap-south-1":{"64p":"ami-82126eed", "64h":"ami-723c401d"},
   "sa-east-1":{"64p":"ami-afd7b9c3", "64h":"ami-ab9af4c7"}}},
 "Parameters":
 {"InstanceType":
  {"Description":"Type of EC2 instance to launch",
   "Type":"String",
   "Default":"c3.large"},
  "InstanceProfile":
  {"Description":"Preexisting IAM role \/ instance profile",
   "Type":"String",
   "Default":"datomic-aws-transactor-4"},
  "Xmx":
  {"Description":"Xmx setting for the JVM",
   "Type":"String",
   "AllowedPattern":"\\d+[GgMm]",
   "Default":"2625m"},
  "GroupSize":
  {"Description":"Size of machine group",
   "Type":"String",
   "Default":"1"},
  "InstanceMonitoring":
  {"Description":"Detailed monitoring for store instances?",
   "Type":"String",
   "Default":"true"},
  "JavaOpts":
  {"Description":"Options passed to Java launcher",
   "Type":"String",
   "Default":""},
  "SecurityGroups":
  {"Description":"Preexisting security groups.",
   "Type":"CommaDelimitedList",
   "Default":"datomic"},
  "DatomicDeployBucket":
  {"Type":"String",
   "Default":"deploy-a0dbc565-faf2-4760-9b7e-29a8e45f428e"},
  "DatomicVersion":{"Type":"String", "Default":"0.9.5561.59"}},
 "Description":"Datomic Transactor Template"}
STACK
}

resource "aws_ecs_cluster" "edgarly-cluster" {
  name = "edgarly"
}

resource "aws_ecs_task_definition" "edgarly-task" {
  family = "edgarly"
  container_definitions = "${file("task-definitions/edgarly.json")}"
  volume {
    name = "edgarly-home"
    host_path = "/ecs/edgarly-home"
  }
}

resource "aws_ecs_service" "edgarly-service" {
  name           = "edgarly-service"
  cluster        = "${aws_ecs_cluster.edgarly-cluster.id}"
  task_definition = "${aws_ecs_task_definition.edgarly-task.arn}"
  desired_count   = 1
}

resource "aws_iam_role" "edgarly" {
    name = "edgarly"
    assume_role_policy = <<EOF
{
  "Version": "2008-10-17",
  "Statement": [
    {
      "Sid": "",
      "Effect": "Allow",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF
}

resource "aws_iam_policy" "edgarly" {
    name = "edgarly"
    path = "/"
    description = "edgarly"
    policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecs:CreateCluster",
        "ecs:DeregisterContainerInstance",
        "ecs:DiscoverPollEndpoint",
        "ecs:Poll",
        "ecs:RegisterContainerInstance",
        "ecs:StartTelemetrySession",
        "ecs:Submit*",
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "*"
    }
  ]
}
EOF
}

resource "aws_iam_policy_attachment" "edgarly" {
    name = "edgarly"
    roles = ["${aws_iam_role.edgarly.name}"]
    policy_arn = "${aws_iam_policy.edgarly.arn}"
}

resource "aws_iam_instance_profile" "edgarly" {
    name = "edgarly"
    roles = ["${aws_iam_role.edgarly.name}"]
}

resource "aws_security_group" "edgarly" {
  name = "edgarly"
  description = "edgarly"

  ingress {
      from_port = 80
      to_port = 80
      protocol = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 8080
    to_port = 8080
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
      from_port = 22
      to_port = 22
      protocol = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
      from_port = 0
      to_port = 0
      protocol = "-1"
      cidr_blocks = ["0.0.0.0/0"]
  }
}

data "aws_instance" "datomic-instance" {
  filter {
    name   = "tag:Name"
    values = ["edgarly-datomic-stack"]
  }
}

resource "aws_instance" "datomic-console-instance" {

  ami = "ami-ec33cc96"
  instance_type = "t2.small"
  key_name = "aws-timothyjwashington-keypair"
  security_groups = ["${aws_security_group.edgarly.name}"]
  iam_instance_profile = "edgarly"

  user_data = "#!/bin/bash\necho ECS_CLUSTER=edgarly >> /etc/ecs/ecs.config ; echo DATOMIC_HOST=${data.aws_instance.datomic-instance.public_ip} > /etc/environment"
  tags {
    Name = "edgarly-custom"
  }
}
