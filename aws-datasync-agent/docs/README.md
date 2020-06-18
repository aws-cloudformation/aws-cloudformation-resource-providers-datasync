# AWS::DataSync::Agent

Resource schema for AWS::DataSync::Agent.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::DataSync::Agent",
    "Properties" : {
        "<a href="#agentname" title="AgentName">AgentName</a>" : <i>String</i>,
        "<a href="#activationkey" title="ActivationKey">ActivationKey</a>" : <i>String</i>,
        "<a href="#securitygrouparns" title="SecurityGroupArns">SecurityGroupArns</a>" : <i>[ String, ... ]</i>,
        "<a href="#subnetarns" title="SubnetArns">SubnetArns</a>" : <i>[ String, ... ]</i>,
        "<a href="#vpcendpointid" title="VpcEndpointId">VpcEndpointId</a>" : <i>String</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tags.md">Tags</a>, ... ]</i>,
    }
}
</pre>

### YAML

<pre>
Type: AWS::DataSync::Agent
Properties:
    <a href="#agentname" title="AgentName">AgentName</a>: <i>String</i>
    <a href="#activationkey" title="ActivationKey">ActivationKey</a>: <i>String</i>
    <a href="#securitygrouparns" title="SecurityGroupArns">SecurityGroupArns</a>: <i>
      - String</i>
    <a href="#subnetarns" title="SubnetArns">SubnetArns</a>: <i>
      - String</i>
    <a href="#vpcendpointid" title="VpcEndpointId">VpcEndpointId</a>: <i>String</i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tags.md">Tags</a></i>
</pre>

## Properties

#### AgentName

The name configured for the agent. Text reference used to identify the agent in the console.

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>256</code>

_Pattern_: <code>^[a-zA-Z0-9\s+=.:@/-]+$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ActivationKey

Activation key of the Agent.

_Required_: No

_Type_: String

_Maximum_: <code>29</code>

_Pattern_: <code>[A-Z0-9]{5}(-[A-Z0-9]{5}){4}</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### SecurityGroupArns

The ARNs of the security group used to protect your data transfer task subnets.

_Required_: No

_Type_: List of String

_Maximum_: <code>128</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SubnetArns

The ARNs of the subnets in which DataSync will create elastic network interfaces for each data transfer task.

_Required_: No

_Type_: List of String

_Maximum_: <code>128</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### VpcEndpointId

The ID of the VPC endpoint that the agent has access to.

_Required_: No

_Type_: String

_Pattern_: <code>^vpce-[0-9a-f]{17}$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Tags

An array of key-value pairs to apply to this resource.

_Required_: No

_Type_: List of <a href="tags.md">Tags</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the AgentArn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### AgentArn

The DataSync Agent ARN.

