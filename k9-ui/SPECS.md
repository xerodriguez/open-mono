# kube kui
The application is a Kubernetes context viewer and manager. It allows users to view and manage their Kubernetes contexts, namespaces, and resources in a tree view. It also allows users to view resource details, YAML, events, logs, metrics, pods, containers, images, volumes, nodes, namespaces, services, deployments, statefulsets, daemonsets, and jobs in a table view.

## Tech stack
- Kotlin 2.3 
- SQLite 

Kotlin + GraalVM

The goal is to have a minimal and efficient application that can run on desktop devices. The application will be built using Kotlin and will leverage the power of GraalVM to create a native executable for desktop platforms. The use of SQLite will allow for efficient storage and retrieval of Kubernetes context data.

## UI
The UI is built using Material Design 3. It is designed to be simple and intuitive, with a focus on usability and accessibility. The UI is responsive and works well on desktop devices.

The UI must be clean and simple.

Layout:
- Left sidebar:  Tree view of Kubernetes contexts, namespaces, and resources.
- Main content area: Table view of resource details, YAML, events, logs, metrics,
- Top bar: Search bar, filter options, and action buttons.

## Features

### kubernetes context 
Have a page view to configure all the kubernetes contexts. The user can access this page from the top bar. The user can add, edit, and delete contexts. The user can also switch between contexts.

Each context will have the following information:
- Name
- context 
- color 
- port forwarding base port 
- sub-contexts (optional)

Color is important to differentiate between contexts in the tree view. The user can choose a color for each context to make it easier to identify them.

Context will be stored in the SQLite database and will be loaded when the application starts. 
The context must be append on every `kubectl` command execution. This will allow the user to have a history of the contexts they have used and easily switch between them.

Each context can have an optional list of sub-contexts. This will allow the user to group related contexts together and easily switch between them. For example, a user might have a group of contexts for different environments (e.g., development, staging, production) and can easily switch between them.

There could be more than one active context at the same time, the application will allow the user to switch between them and view their resources in the tree view. The user can also filter the resources in the tree view by context to only show the resources for a specific context.

### port forwarding
The application will have a port forwarding feature that allows users to forward ports from their local machine to their Kubernetes cluster. This will enable users to access services running in their cluster from their local machine.
The user can persist the port forwarding configuration in the SQLite database. This will allow the user to have a history of the port forwarding configurations they have used and easily switch between them.
The user can also view the status of their port forwarding configurations and stop them when they are no longer needed.
The are several pods running on the same port on the cluster, the application will automatically assign a different local port for each port forwarding configuration to avoid conflicts. The user can also specify a custom local port if they prefer, this preference must be saved in the database for future use.
The application will also have a feature to automatically stop port forwarding configurations after a certain period of time or when the user closes the application to prevent orphaned port forwarding configurations.
The application will have a page to show all the active port forwarding configurations, where the user can view the details of each configuration, such as the local port, remote port, and status. The user can also stop or start any port forwarding configuration from this page.

### Left navigation sidebar

#### Tree view of Kubernetes contexts, namespaces, and resources

The left navigation sidebar will display an accordion for each active context with its namespaces and resources. The user can expand each context to see its namespaces and resources. The user can also search for specific contexts, namespaces, or resources using the search bar in the top bar.

The tree view will be organized in a hierarchical manner, with contexts at the top level, followed by namespaces, and then resources. The user can click on any context, namespace, or resource to view its details in the main content area. The user can also right-click on any context, namespace, or resource to access additional actions such as editing, deleting, or port forwarding.

The tree view will also have a filter option that allows the user to filter the contexts, namespaces, and resources based on specific criteria such as name, type, or status. This will help the user quickly find the context, namespace, or resource they are looking for.

The tree must group resources by type (e.g., pods, services, deployments) under each namespace to make it easier for users to navigate and find specific resources. This grouping will also allow users to quickly identify the types of resources available in each namespace and manage them more efficiently.

Inside the grouping of resources by type, the resources will be sorted alphabetically to make it easier for users to find specific resources. This sorting will help users quickly locate the resource they are looking for, especially when there are many resources of the same type in a namespace. 

The tree view should be updated in real-time to reflect any changes in the Kubernetes cluster, such as the addition or deletion of contexts, namespaces, or resources. This will ensure that users always have an up-to-date view of their Kubernetes environment and can manage their resources effectively.