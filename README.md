# MonDNS
The main use case for MonDNS was for self healing round-robin high availability / load balancing scenarios where a host goes down and is automatically removed from the round-robin.This application is nearly as old as tinydns and I dredged it up for an IoT Edge Device HA use case. Services like cloudflare have massively changed the landscape and diminished the applications of this kind of software to a few niche use cases, one of which happens to be in IoT. It worked great in it's time, but YMMV :D 

## 🛠 Features

✔ **Monitors servers in DNS configs** by checking connectivity to predefined hosts  
✔ **Automatically updates the DNS configuration file** (`tiny_dns_config`) when a server’s status changes  
✔ **Triggers a DNS database rebuild** using a configurable command (`tinydns make`)  
✔ **Runs as a continuous background service** with a configurable interval  
✔ **Graceful shutdown** via `Ctrl+C` or system signals  
✔ **Logs status changes** and connection issues   


## 🖥️ Installation & Setup

### **Prerequisites**
- **Java 11+** (Ensure `java -version` returns at least Java 11)
- **Maven** (Check `mvn -version`)


### **Potential Uses for MonDNS**
A program like **MonDNS** has several applications in networking, server administration, and high-availability (HA) systems.

---

## ** 1 Round-Robin Load Balancing**
🔄 **Use Case:** **Dynamic DNS-based Load Balancing**  
- **How it Works:**  
  - In a **DNS-based load balancing** setup, multiple servers share the same DNS hostname, and the DNS server **round-robins** traffic to different IPs.
  - If one of the servers **goes down**, it should be **removed from the DNS pool** to prevent directing traffic to a dead server.
  - **MonDNS** can:
    - **Detect failed servers** in the DNS configuration.
    - **Update the DNS records** to exclude downed servers.
    - **Trigger a DNS rebuild** to reflect changes in real time.

- **Example:**
  - Suppose a website has **3 web servers** behind a round-robin DNS setup:
    ```
    web.example.com → 192.168.1.10
    web.example.com → 192.168.1.11
    web.example.com → 192.168.1.12
    ```
  - If `192.168.1.11` crashes, **MonDNS** removes it from the config:
    ```
    web.example.com → 192.168.1.10
    web.example.com → 192.168.1.12
    ```
  - This prevents users from being routed to an unavailable server.

✅ **Benefit:** **Automatic failover & self-healing DNS configurations.**  


---

## ** 2 Disaster Recovery & Failover Scenarios**
🚨 **Use Case:** **Automated DNS Failover for Critical Services**  
- Many organizations rely on **DNS failover** to switch services to backup data centers in case of failure.
- **MonDNS** could be used to:
  - Detect when the primary service endpoint fails.
  - Update DNS records to point to a **backup server or secondary datacenter**.
  - Reduce downtime by automating failover **without manual intervention**.

✅ **Benefit:** **Fast response time to failures, reducing manual work.**  

---

## ** 3 Edge Computing & IoT Deployments**
🛰 **Use Case:** **IoT Device Connectivity Management**  
- Many IoT schemas use multiple edge devices in large scale networks
- If an edge device outage occurs, devices can’t function properly or send data.
- **MonDNS** can:
  - Detect failures in edge networks.
  - Automatically reconfigure internal / split horizon DNS to use alternative edge servers.

✅ **Benefit:** **Ensures IoT devices remain connected even if an edge device failure occurs.**  



