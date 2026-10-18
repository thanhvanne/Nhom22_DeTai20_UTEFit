USE [master]
GO
/****** Object:  Database [UTEFitDB]    Script Date: 18/10/2026 1:37:58 CH ******/
CREATE DATABASE [UTEFitDB]
 CONTAINMENT = NONE
 ON  PRIMARY 
( NAME = N'UTEFitDB', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL17.SQLEXPRESS01\MSSQL\DATA\UTEFitDB.mdf' , SIZE = 8192KB , MAXSIZE = UNLIMITED, FILEGROWTH = 65536KB )
 LOG ON 
( NAME = N'UTEFitDB_log', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL17.SQLEXPRESS01\MSSQL\DATA\UTEFitDB_log.ldf' , SIZE = 8192KB , MAXSIZE = 2048GB , FILEGROWTH = 65536KB )
 WITH CATALOG_COLLATION = DATABASE_DEFAULT, LEDGER = OFF
GO
ALTER DATABASE [UTEFitDB] SET COMPATIBILITY_LEVEL = 170
GO
IF (1 = FULLTEXTSERVICEPROPERTY('IsFullTextInstalled'))
begin
EXEC [UTEFitDB].[dbo].[sp_fulltext_database] @action = 'enable'
end
GO
ALTER DATABASE [UTEFitDB] SET ANSI_NULL_DEFAULT OFF 
GO
ALTER DATABASE [UTEFitDB] SET ANSI_NULLS OFF 
GO
ALTER DATABASE [UTEFitDB] SET ANSI_PADDING OFF 
GO
ALTER DATABASE [UTEFitDB] SET ANSI_WARNINGS OFF 
GO
ALTER DATABASE [UTEFitDB] SET ARITHABORT OFF 
GO
ALTER DATABASE [UTEFitDB] SET AUTO_CLOSE ON 
GO
ALTER DATABASE [UTEFitDB] SET AUTO_SHRINK OFF 
GO
ALTER DATABASE [UTEFitDB] SET AUTO_UPDATE_STATISTICS ON 
GO
ALTER DATABASE [UTEFitDB] SET CURSOR_CLOSE_ON_COMMIT OFF 
GO
ALTER DATABASE [UTEFitDB] SET CURSOR_DEFAULT  GLOBAL 
GO
ALTER DATABASE [UTEFitDB] SET CONCAT_NULL_YIELDS_NULL OFF 
GO
ALTER DATABASE [UTEFitDB] SET NUMERIC_ROUNDABORT OFF 
GO
ALTER DATABASE [UTEFitDB] SET QUOTED_IDENTIFIER OFF 
GO
ALTER DATABASE [UTEFitDB] SET RECURSIVE_TRIGGERS OFF 
GO
ALTER DATABASE [UTEFitDB] SET  ENABLE_BROKER 
GO
ALTER DATABASE [UTEFitDB] SET AUTO_UPDATE_STATISTICS_ASYNC OFF 
GO
ALTER DATABASE [UTEFitDB] SET DATE_CORRELATION_OPTIMIZATION OFF 
GO
ALTER DATABASE [UTEFitDB] SET TRUSTWORTHY OFF 
GO
ALTER DATABASE [UTEFitDB] SET ALLOW_SNAPSHOT_ISOLATION OFF 
GO
ALTER DATABASE [UTEFitDB] SET PARAMETERIZATION SIMPLE 
GO
ALTER DATABASE [UTEFitDB] SET READ_COMMITTED_SNAPSHOT OFF 
GO
ALTER DATABASE [UTEFitDB] SET HONOR_BROKER_PRIORITY OFF 
GO
ALTER DATABASE [UTEFitDB] SET RECOVERY SIMPLE 
GO
ALTER DATABASE [UTEFitDB] SET  MULTI_USER 
GO
ALTER DATABASE [UTEFitDB] SET PAGE_VERIFY CHECKSUM  
GO
ALTER DATABASE [UTEFitDB] SET DB_CHAINING OFF 
GO
ALTER DATABASE [UTEFitDB] SET FILESTREAM( NON_TRANSACTED_ACCESS = OFF ) 
GO
ALTER DATABASE [UTEFitDB] SET TARGET_RECOVERY_TIME = 60 SECONDS 
GO
ALTER DATABASE [UTEFitDB] SET DELAYED_DURABILITY = DISABLED 
GO
ALTER DATABASE [UTEFitDB] SET OPTIMIZED_LOCKING = OFF 
GO
ALTER DATABASE [UTEFitDB] SET ACCELERATED_DATABASE_RECOVERY = OFF  
GO
ALTER DATABASE [UTEFitDB] SET QUERY_STORE = ON
GO
ALTER DATABASE [UTEFitDB] SET QUERY_STORE (OPERATION_MODE = READ_WRITE, CLEANUP_POLICY = (STALE_QUERY_THRESHOLD_DAYS = 30), DATA_FLUSH_INTERVAL_SECONDS = 900, INTERVAL_LENGTH_MINUTES = 60, MAX_STORAGE_SIZE_MB = 1000, QUERY_CAPTURE_MODE = AUTO, SIZE_BASED_CLEANUP_MODE = AUTO, MAX_PLANS_PER_QUERY = 200, WAIT_STATS_CAPTURE_MODE = ON)
GO
USE [UTEFitDB]
GO
/****** Object:  Table [dbo].[branches]    Script Date: 18/10/2026 1:37:59 CH ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[branches](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [active] [bit] NOT NULL,
    [address] [nvarchar](255) NOT NULL,
    [closing_time] [time](7) NULL,
    [created_at] [datetime2](7) NOT NULL,
    [description] [nvarchar](1000) NULL,
    [image_public_id] [varchar](255) NULL,
    [image_url] [varchar](255) NULL,
    [name] [nvarchar](150) NOT NULL,
    [opening_time] [time](7) NULL,
    [phone] [varchar](20) NULL,
    [updated_at] [datetime2](7) NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[class_categories]    Script Date: 18/10/2026 1:37:59 CH ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[class_categories](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [active] [bit] NOT NULL,
    [created_at] [datetime2](7) NOT NULL,
    [description] [nvarchar](1000) NULL,
    [name] [nvarchar](100) NOT NULL,
    [updated_at] [datetime2](7) NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[class_registrations]    Script Date: 18/10/2026 1:37:59 CH ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[class_registrations](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [registered_at] [datetime2](7) NOT NULL,
    [status] [varchar](20) NOT NULL,
    [schedule_id] [bigint] NOT NULL,
    [user_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[class_schedules]    Script Date: 18/10/2026 1:37:59 CH ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[class_schedules](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [active] [bit] NOT NULL,
    [created_at] [datetime2](7) NOT NULL,
    [end_time] [time](7) NOT NULL,
    [room] [nvarchar](100) NULL,
    [schedule_date] [date] NOT NULL,
    [start_time] [time](7) NOT NULL,
    [fitness_class_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[fitness_classes]    Script Date: 18/10/2026 1:37:59 CH ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[fitness_classes](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [capacity] [int] NOT NULL,
    [created_at] [datetime2](7) NOT NULL,
    [description] [nvarchar](1500) NULL,
    [name] [nvarchar](150) NOT NULL,
    [status] [varchar](20) NOT NULL,
    [updated_at] [datetime2](7) NULL,
    [branch_id] [bigint] NOT NULL,
    [category_id] [bigint] NOT NULL,
    [trainer_id] [bigint] NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[membership_plans]    Script Date: 18/10/2026 1:37:59 CH ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[membership_plans](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [active] [bit] NOT NULL,
    [created_at] [datetime2](7) NOT NULL,
    [description] [nvarchar](1500) NULL,
    [duration_days] [int] NOT NULL,
    [name] [nvarchar](100) NOT NULL,
    [price] [numeric](15, 2) NOT NULL,
    [updated_at] [datetime2](7) NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[memberships]    Script Date: 18/10/2026 1:37:59 CH ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[memberships](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [created_at] [datetime2](7) NOT NULL,
    [end_date] [date] NULL,
    [start_date] [date] NULL,
    [status] [varchar](20) NOT NULL,
    [updated_at] [datetime2](7) NULL,
    [branch_id] [bigint] NOT NULL,
    [plan_id] [bigint] NOT NULL,
    [user_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[otp_codes]    Script Date: 18/10/2026 1:37:59 CH ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[otp_codes](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [code] [varchar](100) NULL,
    [created_at] [datetime2](7) NOT NULL,
    [expired_at] [datetime2](7) NOT NULL,
    [type] [varchar](30) NOT NULL,
    [used] [bit] NOT NULL,
    [used_at] [datetime2](7) NULL,
    [user_id] [bigint] NOT NULL,
    [attempt_count] [int] NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[payments]    Script Date: 18/10/2026 1:37:59 CH ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[payments](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [amount] [numeric](15, 2) NOT NULL,
    [created_at] [datetime2](7) NOT NULL,
    [method] [varchar](20) NOT NULL,
    [paid_at] [datetime2](7) NULL,
    [status] [varchar](20) NOT NULL,
    [transaction_code] [varchar](100) NULL,
    [membership_id] [bigint] NULL,
    [user_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[reviews]    Script Date: 18/10/2026 1:37:59 CH ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[reviews](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [comment] [nvarchar](2000) NULL,
    [created_at] [datetime2](7) NOT NULL,
    [rating] [int] NOT NULL,
    [updated_at] [datetime2](7) NULL,
    [visible] [bit] NOT NULL,
    [branch_id] [bigint] NOT NULL,
    [trainer_id] [bigint] NULL,
    [user_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[trainers]    Script Date: 18/10/2026 1:37:59 CH ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[trainers](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [active] [bit] NOT NULL,
    [bio] [nvarchar](2000) NULL,
    [created_at] [datetime2](7) NOT NULL,
    [specialization] [nvarchar](255) NULL,
    [updated_at] [datetime2](7) NULL,
    [years_experience] [int] NULL,
    [branch_id] [bigint] NULL,
    [user_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[users]    Script Date: 18/10/2026 1:37:59 CH ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[users](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [avatar_public_id] [varchar](255) NULL,
    [avatar_url] [varchar](255) NULL,
    [created_at] [datetime2](7) NOT NULL,
    [email] [varchar](100) NOT NULL,
    [enabled] [bit] NOT NULL,
    [first_name] [nvarchar](50) NOT NULL,
    [last_name] [nvarchar](50) NOT NULL,
    [password] [varchar](255) NOT NULL,
    [phone] [varchar](20) NULL,
    [role] [varchar](20) NOT NULL,
    [status] [varchar](20) NOT NULL,
    [updated_at] [datetime2](7) NULL,
    [token_version] [int] NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
    SET IDENTITY_INSERT [dbo].[branches] ON

    INSERT [dbo].[branches] ([id], [active], [address], [closing_time], [created_at], [description], [image_public_id], [image_url], [name], [opening_time], [phone], [updated_at]) VALUES (1, 1, N'01 Võ Văn Ngân, TP. Thủ Đức', CAST(N'18:00:00' AS Time), CAST(N'2026-10-17T20:14:49.9815413' AS DateTime2), N'', N'utefit/branches/h2qqmefhur7mwwv1zvtf', N'https://res.cloudinary.com/tburyrz8/image/upload/v1792263613/utefit/branches/h2qqmefhur7mwwv1zvtf.jpg', N'UTEFit - Thủ Đức', CAST(N'00:00:00' AS Time), N'0397015250', CAST(N'2026-10-18T02:00:13.8976068' AS DateTime2))
    INSERT [dbo].[branches] ([id], [active], [address], [closing_time], [created_at], [description], [image_public_id], [image_url], [name], [opening_time], [phone], [updated_at]) VALUES (2, 1, N'123 Lê Lợi, Quận 1, TP. HCM', CAST(N'22:00:00' AS Time), CAST(N'2026-10-18T13:25:48.1411699' AS DateTime2), N'Chi nhánh trung tâm.', NULL, NULL, N'UTEFit Quận 1', CAST(N'06:00:00' AS Time), N'0901 000 002', CAST(N'2026-10-18T13:25:48.1411699' AS DateTime2))
    INSERT [dbo].[branches] ([id], [active], [address], [closing_time], [created_at], [description], [image_public_id], [image_url], [name], [opening_time], [phone], [updated_at]) VALUES (3, 1, N'45 Điện Biên Phủ, Bình Thạnh, TP. HCM', CAST(N'22:00:00' AS Time), CAST(N'2026-10-18T13:25:48.1411699' AS DateTime2), N'Chi nhánh Bình Thạnh.', NULL, NULL, N'UTEFit Bình Thạnh', CAST(N'06:00:00' AS Time), N'0901 000 003', CAST(N'2026-10-18T13:25:48.1411699' AS DateTime2))
    SET IDENTITY_INSERT [dbo].[branches] OFF
    GO
    SET IDENTITY_INSERT [dbo].[otp_codes] ON

    INSERT [dbo].[otp_codes] ([id], [code], [created_at], [expired_at], [type], [used], [used_at], [user_id], [attempt_count]) VALUES (5, N'$2a$10$pfU7p/BzA07YuBwrnIsZ1OC0BnsP8RHR33wePzQNBGUCm7.ToIDH6', CAST(N'2026-10-17T21:59:59.7137881' AS DateTime2), CAST(N'2026-10-17T22:04:59.7137881' AS DateTime2), N'REGISTER', 1, CAST(N'2026-10-17T22:00:27.5414349' AS DateTime2), 8, 0)
    SET IDENTITY_INSERT [dbo].[otp_codes] OFF
    GO
    SET IDENTITY_INSERT [dbo].[trainers] ON

    INSERT [dbo].[trainers] ([id], [active], [bio], [created_at], [specialization], [updated_at], [years_experience], [branch_id], [user_id]) VALUES (1, 1, N'Chuyên về Yoga cơ bản, phục hồi, giãn cơ cho sinh viên và người mới bắt đầu.', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), N'Yoga & Mobility', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), 5, 1, 1)
    INSERT [dbo].[trainers] ([id], [active], [bio], [created_at], [specialization], [updated_at], [years_experience], [branch_id], [user_id]) VALUES (2, 1, N'Huấn luyện viên thể hình cơ bản, tập trung vào giảm mỡ và cải thiện sức khỏe tổng quát.', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), N'Gym cơ bản & giảm mỡ', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), 3, 1, 2)
    INSERT [dbo].[trainers] ([id], [active], [bio], [created_at], [specialization], [updated_at], [years_experience], [branch_id], [user_id]) VALUES (3, 1, N'Chuyên về sức mạnh, kỹ thuật squat, bench press, deadlift.', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), N'Powerlifting & Strength', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), 6, 2, 3)
    INSERT [dbo].[trainers] ([id], [active], [bio], [created_at], [specialization], [updated_at], [years_experience], [branch_id], [user_id]) VALUES (4, 1, N'Kinh nghiệm thi đấu Boxing phong trào, phù hợp cho học viên muốn vừa học võ vừa giảm mỡ.', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), N'Boxing & Conditioning', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), 4, 2, 4)
    INSERT [dbo].[trainers] ([id], [active], [bio], [created_at], [specialization], [updated_at], [years_experience], [branch_id], [user_id]) VALUES (5, 1, N'Tập trung vào luyện tập chức năng, cải thiện sức bền và thể lực toàn diện.', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), N'CrossFit & Functional Training', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), 5, 3, 5)
    INSERT [dbo].[trainers] ([id], [active], [bio], [created_at], [specialization], [updated_at], [years_experience], [branch_id], [user_id]) VALUES (6, 1, N'Phù hợp với học viên muốn cải thiện tim mạch, sức bền, chuẩn bị cho các giải chạy.', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), N'Cardio & Endurance', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), 2, 3, 6)
    INSERT [dbo].[trainers] ([id], [active], [bio], [created_at], [specialization], [updated_at], [years_experience], [branch_id], [user_id]) VALUES (7, 1, N'Hướng dẫn chế độ tập luyện và dinh dưỡng cho học viên muốn giảm mỡ an toàn.', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), N'Fat Loss & Body Recomposition', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), 7, 1, 7)
    INSERT [dbo].[trainers] ([id], [active], [bio], [created_at], [specialization], [updated_at], [years_experience], [branch_id], [user_id]) VALUES (8, 1, N'Kinh nghiệm làm việc với sinh viên gầy, muốn tăng cân và tăng cơ.', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), N'Hypertrophy & Muscle Gain', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), 4, 2, 8)
    INSERT [dbo].[trainers] ([id], [active], [bio], [created_at], [specialization], [updated_at], [years_experience], [branch_id], [user_id]) VALUES (9, 1, N'Phù hợp cho học viên cần giãn cơ, cải thiện tư thế và giảm stress.', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), N'Yoga & Stretching', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), 3, NULL, 9)
    INSERT [dbo].[trainers] ([id], [active], [bio], [created_at], [specialization], [updated_at], [years_experience], [branch_id], [user_id]) VALUES (10, 1, N'Tài khoản đang bị khóa – dùng để test filter public trainer.', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), N'Functional Training', CAST(N'2026-10-18T13:25:52.5102982' AS DateTime2), 5, 1, 10)
    SET IDENTITY_INSERT [dbo].[trainers] OFF
    GO
    SET IDENTITY_INSERT [dbo].[users] ON

    INSERT [dbo].[users] ([id], [avatar_public_id], [avatar_url], [created_at], [email], [enabled], [first_name], [last_name], [password], [phone], [role], [status], [updated_at], [token_version]) VALUES (1, NULL, NULL, CAST(N'2026-10-17T20:23:58.3033333' AS DateTime2), N'admin@utefit.com', 1, N'Trường', N'Nguyễn Quang', N'$2a$10$Dow1z2y3x4w5v6u7t8s9r0q1p2o3n4m5l6k7j8i9h0g1f2e3d4c5b', N'0901112222', N'ADMIN', N'ACTIVE', CAST(N'2026-10-17T22:07:31.0722490' AS DateTime2), NULL)
    INSERT [dbo].[users] ([id], [avatar_public_id], [avatar_url], [created_at], [email], [enabled], [first_name], [last_name], [password], [phone], [role], [status], [updated_at], [token_version]) VALUES (2, NULL, NULL, CAST(N'2026-10-17T20:23:58.3033333' AS DateTime2), N'pt.duy@utefit.com', 1, N'Duy', N'Lê Hoàng', N'$2a$10$Dow1z2y3x4w5v6u7t8s9r0q1p2o3n4m5l6k7j8i9h0g1f2e3d4c5b', N'0903334444', N'PT', N'ACTIVE', CAST(N'2026-10-17T20:23:58.3033333' AS DateTime2), NULL)
    INSERT [dbo].[users] ([id], [avatar_public_id], [avatar_url], [created_at], [email], [enabled], [first_name], [last_name], [password], [phone], [role], [status], [updated_at], [token_version]) VALUES (3, NULL, NULL, CAST(N'2026-10-17T20:23:58.3033333' AS DateTime2), N'member.an@utefit.com', 1, N'An', N'Trần Bình', N'$2a$10$Dow1z2y3x4w5v6u7t8s9r0q1p2o3n4m5l6k7j8i9h0g1f2e3d4c5b', N'0905556666', N'MEMBER', N'ACTIVE', CAST(N'2026-10-17T20:23:58.3033333' AS DateTime2), NULL)
    INSERT [dbo].[users] ([id], [avatar_public_id], [avatar_url], [created_at], [email], [enabled], [first_name], [last_name], [password], [phone], [role], [status], [updated_at], [token_version]) VALUES (4, NULL, NULL, CAST(N'2026-10-18T13:24:44.1367599' AS DateTime2), N'huy.tran@utefit.vn', 0, N'Huy', N'Trần', N'123456', N'0902 111 004', N'PT', N'LOCKED', CAST(N'2026-10-18T13:30:12.1681795' AS DateTime2), NULL)
    INSERT [dbo].[users] ([id], [avatar_public_id], [avatar_url], [created_at], [email], [enabled], [first_name], [last_name], [password], [phone], [role], [status], [updated_at], [token_version]) VALUES (5, NULL, NULL, CAST(N'2026-10-18T13:24:44.1367599' AS DateTime2), N'lan.phan@utefit.vn', 1, N'Lan', N'Phan', N'123456', N'0902 111 005', N'PT', N'ACTIVE', CAST(N'2026-10-18T13:24:44.1367599' AS DateTime2), NULL)
    INSERT [dbo].[users] ([id], [avatar_public_id], [avatar_url], [created_at], [email], [enabled], [first_name], [last_name], [password], [phone], [role], [status], [updated_at], [token_version]) VALUES (6, NULL, NULL, CAST(N'2026-10-18T13:24:44.1367599' AS DateTime2), N'tuan.hoang@utefit.vn', 1, N'Tuấn', N'Hoàng', N'123456', N'0902 111 006', N'PT', N'ACTIVE', CAST(N'2026-10-18T13:24:44.1367599' AS DateTime2), NULL)
    INSERT [dbo].[users] ([id], [avatar_public_id], [avatar_url], [created_at], [email], [enabled], [first_name], [last_name], [password], [phone], [role], [status], [updated_at], [token_version]) VALUES (7, NULL, NULL, CAST(N'2026-10-18T13:24:44.1367599' AS DateTime2), N'anh.vo@utefit.vn', 1, N'Anh', N'Võ', N'123456', N'0902 111 007', N'PT', N'ACTIVE', CAST(N'2026-10-18T13:24:44.1367599' AS DateTime2), NULL)
    INSERT [dbo].[users] ([id], [avatar_public_id], [avatar_url], [created_at], [email], [enabled], [first_name], [last_name], [password], [phone], [role], [status], [updated_at], [token_version]) VALUES (8, NULL, NULL, CAST(N'2026-10-17T21:59:59.6247771' AS DateTime2), N'24110367@student.hcmute.edu.vn', 1, N'Nguyễn', N'Trường', N'$2a$10$AIOSSbTsdHWY53Y2QkEiPu/QaoNqEMsbvRwdKmOlfc8cAOMK6BmJi', N'0397015250', N'ADMIN', N'ACTIVE', CAST(N'2026-10-18T13:30:17.1215003' AS DateTime2), 5)
    INSERT [dbo].[users] ([id], [avatar_public_id], [avatar_url], [created_at], [email], [enabled], [first_name], [last_name], [password], [phone], [role], [status], [updated_at], [token_version]) VALUES (9, NULL, NULL, CAST(N'2026-10-18T13:24:44.1367599' AS DateTime2), N'thao.bui@utefit.vn', 1, N'Thảo', N'Bùi', N'123456', N'0902 111 009', N'PT', N'ACTIVE', CAST(N'2026-10-18T13:24:44.1367599' AS DateTime2), NULL)
    INSERT [dbo].[users] ([id], [avatar_public_id], [avatar_url], [created_at], [email], [enabled], [first_name], [last_name], [password], [phone], [role], [status], [updated_at], [token_version]) VALUES (10, NULL, NULL, CAST(N'2026-10-18T13:24:44.1367599' AS DateTime2), N'hung.ngo@utefit.vn', 1, N'Hùng', N'Ngô', N'123456', N'0902 111 010', N'PT', N'LOCKED', CAST(N'2026-10-18T13:24:44.1367599' AS DateTime2), NULL)
    SET IDENTITY_INSERT [dbo].[users] OFF
    GO
/****** Object:  Index [uk_registration_user_schedule]    Script Date: 18/10/2026 1:37:59 CH ******/
ALTER TABLE [dbo].[class_registrations] ADD  CONSTRAINT [uk_registration_user_schedule] UNIQUE NONCLUSTERED
    (
    [user_id] ASC,
    [schedule_id] ASC
    )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    GO
/****** Object:  Index [uk_trainers_user]    Script Date: 18/10/2026 1:37:59 CH ******/
ALTER TABLE [dbo].[trainers] ADD  CONSTRAINT [uk_trainers_user] UNIQUE NONCLUSTERED
    (
    [user_id] ASC
    )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    GO
/****** Object:  Index [UX_trainers_user]    Script Date: 18/10/2026 1:37:59 CH ******/
CREATE UNIQUE NONCLUSTERED INDEX [UX_trainers_user] ON [dbo].[trainers]
(
	[user_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [uk_users_email]    Script Date: 18/10/2026 1:37:59 CH ******/
ALTER TABLE [dbo].[users] ADD  CONSTRAINT [uk_users_email] UNIQUE NONCLUSTERED
    (
    [email] ASC
    )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    GO
    SET ANSI_PADDING ON
    GO
/****** Object:  Index [UX_users_email]    Script Date: 18/10/2026 1:37:59 CH ******/
CREATE UNIQUE NONCLUSTERED INDEX [UX_users_email] ON [dbo].[users]
(
	[email] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
ALTER TABLE [dbo].[class_registrations]  WITH CHECK ADD  CONSTRAINT [fk_registration_schedule] FOREIGN KEY([schedule_id])
    REFERENCES [dbo].[class_schedules] ([id])
    GO
ALTER TABLE [dbo].[class_registrations] CHECK CONSTRAINT [fk_registration_schedule]
    GO
ALTER TABLE [dbo].[class_registrations]  WITH CHECK ADD  CONSTRAINT [fk_registration_user] FOREIGN KEY([user_id])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[class_registrations] CHECK CONSTRAINT [fk_registration_user]
    GO
ALTER TABLE [dbo].[class_schedules]  WITH CHECK ADD  CONSTRAINT [fk_schedule_class] FOREIGN KEY([fitness_class_id])
    REFERENCES [dbo].[fitness_classes] ([id])
    GO
ALTER TABLE [dbo].[class_schedules] CHECK CONSTRAINT [fk_schedule_class]
    GO
ALTER TABLE [dbo].[fitness_classes]  WITH CHECK ADD  CONSTRAINT [fk_class_branch] FOREIGN KEY([branch_id])
    REFERENCES [dbo].[branches] ([id])
    GO
ALTER TABLE [dbo].[fitness_classes] CHECK CONSTRAINT [fk_class_branch]
    GO
ALTER TABLE [dbo].[fitness_classes]  WITH CHECK ADD  CONSTRAINT [fk_class_category] FOREIGN KEY([category_id])
    REFERENCES [dbo].[class_categories] ([id])
    GO
ALTER TABLE [dbo].[fitness_classes] CHECK CONSTRAINT [fk_class_category]
    GO
ALTER TABLE [dbo].[fitness_classes]  WITH CHECK ADD  CONSTRAINT [fk_class_trainer] FOREIGN KEY([trainer_id])
    REFERENCES [dbo].[trainers] ([id])
    GO
ALTER TABLE [dbo].[fitness_classes] CHECK CONSTRAINT [fk_class_trainer]
    GO
ALTER TABLE [dbo].[memberships]  WITH CHECK ADD  CONSTRAINT [fk_membership_branch] FOREIGN KEY([branch_id])
    REFERENCES [dbo].[branches] ([id])
    GO
ALTER TABLE [dbo].[memberships] CHECK CONSTRAINT [fk_membership_branch]
    GO
ALTER TABLE [dbo].[memberships]  WITH CHECK ADD  CONSTRAINT [fk_membership_plan] FOREIGN KEY([plan_id])
    REFERENCES [dbo].[membership_plans] ([id])
    GO
ALTER TABLE [dbo].[memberships] CHECK CONSTRAINT [fk_membership_plan]
    GO
ALTER TABLE [dbo].[memberships]  WITH CHECK ADD  CONSTRAINT [fk_membership_user] FOREIGN KEY([user_id])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[memberships] CHECK CONSTRAINT [fk_membership_user]
    GO
ALTER TABLE [dbo].[otp_codes]  WITH CHECK ADD  CONSTRAINT [fk_otp_user] FOREIGN KEY([user_id])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[otp_codes] CHECK CONSTRAINT [fk_otp_user]
    GO
ALTER TABLE [dbo].[payments]  WITH CHECK ADD  CONSTRAINT [fk_payment_membership] FOREIGN KEY([membership_id])
    REFERENCES [dbo].[memberships] ([id])
    GO
ALTER TABLE [dbo].[payments] CHECK CONSTRAINT [fk_payment_membership]
    GO
ALTER TABLE [dbo].[payments]  WITH CHECK ADD  CONSTRAINT [fk_payment_user] FOREIGN KEY([user_id])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[payments] CHECK CONSTRAINT [fk_payment_user]
    GO
ALTER TABLE [dbo].[reviews]  WITH CHECK ADD  CONSTRAINT [fk_review_branch] FOREIGN KEY([branch_id])
    REFERENCES [dbo].[branches] ([id])
    GO
ALTER TABLE [dbo].[reviews] CHECK CONSTRAINT [fk_review_branch]
    GO
ALTER TABLE [dbo].[reviews]  WITH CHECK ADD  CONSTRAINT [fk_review_trainer] FOREIGN KEY([trainer_id])
    REFERENCES [dbo].[trainers] ([id])
    GO
ALTER TABLE [dbo].[reviews] CHECK CONSTRAINT [fk_review_trainer]
    GO
ALTER TABLE [dbo].[reviews]  WITH CHECK ADD  CONSTRAINT [fk_review_user] FOREIGN KEY([user_id])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[reviews] CHECK CONSTRAINT [fk_review_user]
    GO
ALTER TABLE [dbo].[trainers]  WITH CHECK ADD  CONSTRAINT [fk_trainer_branch] FOREIGN KEY([branch_id])
    REFERENCES [dbo].[branches] ([id])
    GO
ALTER TABLE [dbo].[trainers] CHECK CONSTRAINT [fk_trainer_branch]
    GO
ALTER TABLE [dbo].[trainers]  WITH CHECK ADD  CONSTRAINT [fk_trainer_user] FOREIGN KEY([user_id])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[trainers] CHECK CONSTRAINT [fk_trainer_user]
    GO
ALTER TABLE [dbo].[trainers]  WITH CHECK ADD  CONSTRAINT [FK_trainers_branches] FOREIGN KEY([branch_id])
    REFERENCES [dbo].[branches] ([id])
    GO
ALTER TABLE [dbo].[trainers] CHECK CONSTRAINT [FK_trainers_branches]
    GO
ALTER TABLE [dbo].[trainers]  WITH CHECK ADD  CONSTRAINT [FK_trainers_users] FOREIGN KEY([user_id])
    REFERENCES [dbo].[users] ([id])
    ON DELETE CASCADE
GO
ALTER TABLE [dbo].[trainers] CHECK CONSTRAINT [FK_trainers_users]
    GO
ALTER TABLE [dbo].[class_registrations]  WITH CHECK ADD CHECK  (([status]='ABSENT' OR [status]='ATTENDED' OR [status]='CANCELLED' OR [status]='REGISTERED'))
    GO
ALTER TABLE [dbo].[fitness_classes]  WITH CHECK ADD CHECK  (([status]='COMPLETED' OR [status]='CANCELLED' OR [status]='FULL' OR [status]='OPEN'))
    GO
ALTER TABLE [dbo].[memberships]  WITH CHECK ADD CHECK  (([status]='CANCELLED' OR [status]='EXPIRED' OR [status]='ACTIVE' OR [status]='PENDING'))
    GO
ALTER TABLE [dbo].[otp_codes]  WITH CHECK ADD CHECK  (([type]='FORGOT_PASSWORD' OR [type]='REGISTER'))
    GO
ALTER TABLE [dbo].[payments]  WITH CHECK ADD CHECK  (([method]='VNPAY' OR [method]='CASH'))
    GO
ALTER TABLE [dbo].[payments]  WITH CHECK ADD CHECK  (([status]='REFUNDED' OR [status]='FAILED' OR [status]='PAID' OR [status]='PENDING'))
    GO
ALTER TABLE [dbo].[users]  WITH CHECK ADD CHECK  (([role]='ADMIN' OR [role]='PT' OR [role]='MEMBER'))
    GO
ALTER TABLE [dbo].[users]  WITH CHECK ADD CHECK  (([status]='LOCKED' OR [status]='ACTIVE' OR [status]='UNVERIFIED'))
    GO
    USE [master]
    GO
ALTER DATABASE [UTEFitDB] SET  READ_WRITE 
GO
